package com.example.ptnet.interfaces

import com.example.ptnet.models.ConnectionDescriptor


interface ConnectionsListener {
    fun connectionsChanges(numOfConnection: Int)
    fun connectionsAdded(start: Int, descriptorArray: Array<ConnectionDescriptor?>?)
    fun connectionsRemoved(start: Int, descriptorArray: Array<ConnectionDescriptor?>?)
    fun connectionsUpdated(positions: IntArray?)
}