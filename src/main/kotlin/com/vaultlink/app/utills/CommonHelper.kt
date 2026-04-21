package com.vaultlink.app.utills

import com.vaultlink.app.networking.SFConnection
import org.json.JSONArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CommonHelper(@Autowired val sfConnection: SFConnection) {

    fun executeQuery(query: String): JSONArray? {
        val response = sfConnection.get(query) ?: return null
        return response.optJSONArray("records")
    }

    fun escape(input: String): String {
        return input.replace("'", "\\'")
    }
}