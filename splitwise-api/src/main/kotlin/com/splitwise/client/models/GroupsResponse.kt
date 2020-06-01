package com.splitwise.client.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class GroupsResponse(val groups: List<Group>)

@JsonClass(generateAdapter = false)
data class Group(val id: Int, val name: String)
