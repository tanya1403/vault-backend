package com.vaultlink.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@SpringBootApplication
@EnableScheduling
class VaultLinkApplication

fun main(args: Array<String>) {
    runApplication<VaultLinkApplication>(*args)
}
