package org.example.process_scheduling

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform