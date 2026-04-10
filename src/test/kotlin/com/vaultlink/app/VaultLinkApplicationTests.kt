package com.vaultlink.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.vaultlink.app.dto.LoginRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VaultLinkApplicationTests {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private val loginPath = "/vault/v1/login"
    private val validUsername = "admin"
    private val validPassword = "Admin@123"

    @Test
    fun `context loads`() {}

    @Test
    fun `login with valid credentials returns token`() {
        mockMvc.perform(
            post(loginPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    LoginRequest(username = validUsername, password = validPassword)
                ))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty)
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.data.user.username").value(validUsername))
    }

    @Test
    fun `login with invalid credentials returns 401`() {
        mockMvc.perform(
            post(loginPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    LoginRequest(username = "wrong-user", password = "WrongPass123")
                ))
        )
        .andExpect(status().isUnauthorized)
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Invalid username or password"))
    }

    @Test
    fun `login validation returns 400 for blank username`() {
        mockMvc.perform(
            post(loginPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    LoginRequest(username = "", password = validPassword)
                ))
        )
        .andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("username is required"))
    }

    @Test
    fun `refresh with valid token returns new access token`() {
        val loginResponse = mockMvc.perform(
            post(loginPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        LoginRequest(username = validUsername, password = validPassword)
                    )
                )
        ).andReturn().response.contentAsString

        val refreshToken = objectMapper.readTree(loginResponse)
            .path("data")
            .path("refreshToken")
            .asText()

        mockMvc.perform(
            post("/vault/v1/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refreshToken"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
    }
}
