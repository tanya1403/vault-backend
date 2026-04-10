package com.vaultlink.app.utills

import java.util.logging.Logger

object LoggerUtils {

    private val logger: Logger = Logger.getLogger(LoggerUtils::class.java.simpleName)

    fun log(value: String) {
        logger.info("\n\nVaultLink - Value --> $value\n\n")
    }

    fun logBody(body: String) {
        logger.info("\n\nVaultLink - Body --> $body\n\n")
    }

    fun logMethodCall(value: String) {
        logger.info("\nVaultLink -\n----------------------\n  Method --> $value  \n----------------------\n\n")
    }

    fun printLog(value: String) {
        println("\n\nVaultLink - Value --> $value\n\n")
    }

}