package com.h0tk3y.orbiters

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import kotlin.math.PI
import kotlin.math.sqrt

const val gConst = 700f

fun moveShips(
    units: Array<OrbitalUnit>,
    center: Vector2,
    realTimeDelta: Float,
) {
    val gameTimeDelta = realTimeDelta * gameSecondsInRealSecond
    for (unit in units) {
        moveShip(unit, center, gameTimeDelta, outputPointFloats = null)
    }
}

fun moveShip(
    unit: OrbitalUnit,
    center: Vector2,
    gameTimeDelta: Float,
    outputPointFloats: FloatArray?
) {
    val r = Vector2()

    val outputInterval = gameTimeDelta / (outputPointFloats?.size?.toFloat() ?: 1f)

    var gameTimeDeltaRemaining = gameTimeDelta
    var outputIntervalRemaining = 0f

    outputPointFloats?.clear()

    while (gameTimeDeltaRemaining > 0) {
        gameTimeDeltaRemaining -= movementIntegrationStep

        val dt = movementIntegrationStep + if (gameTimeDeltaRemaining < 0f) gameTimeDeltaRemaining else 0f

        // x' = x + v(t) * dt/2
        r.set(unit.velocity)
        r.scl(dt / 2)
        unit.position.add(r)

        // v += a(t + dt/2, x') * dt
        r.set(center)
        r.sub(unit.position)
        val r2 = r.len2()
        val gf = gConst / r2
        r.setLength(gf * dt)
        unit.velocity.add(r)

        // x' = x + v(t + dt/2) * dt/2
        r.set(unit.velocity)
        r.scl(dt / 2)
        unit.position.add(r)

        // Add coordinates to the output array?
        if (outputPointFloats != null) {
            outputIntervalRemaining -= movementIntegrationStep
            if (outputIntervalRemaining < 0.001f) {
                outputIntervalRemaining += outputInterval
                outputPointFloats.add(unit.x, unit.y)
            }
        }
    }
}

private const val movementIntegrationStep = 1f

fun orbitalPeriod(orbitalUnit: OrbitalUnit, center: Vector2): Float {
    val r = Vector2(orbitalUnit.position).sub(center).len()
    val v2 = orbitalUnit.velocity.len2().toDouble() // try to use Double to increase precision?
    val a = 1 / (2 / r - v2 / gConst)
    return (PI * 2.0 * sqrt(a * a * a / gConst)).toFloat()
}

fun calculateOrbitPoints(
    orbitalUnit: OrbitalUnit,
    center: Vector2,
    maxSegments: Int,
    outputPointFloats: FloatArray,
) {
    val originalPosition = orbitalUnit.position.cpy()
    val originalVelocity = orbitalUnit.velocity.cpy()

    val orbitalPeriod = orbitalPeriod(orbitalUnit, center)
    if (orbitalPeriod > maxOrbitalPeriodCutoff) {
        outputPointFloats.setSize(0)
        return
    }

    outputPointFloats.add(orbitalUnit.x, orbitalUnit.y)

    outputPointFloats.setSize(maxSegments * 2)
    moveShip(orbitalUnit, center, orbitalPeriod, outputPointFloats)

    orbitalUnit.position.set(originalPosition)
    orbitalUnit.velocity.set(originalVelocity)

    if (outputPointFloats.size > 0) {
        outputPointFloats.add(orbitalUnit.x, orbitalUnit.y)
    }
}

private const val maxOrbitalPeriodCutoff = 100_000f