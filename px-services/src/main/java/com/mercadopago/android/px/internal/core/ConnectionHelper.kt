package com.mercadopago.android.px.internal.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Collections

class ConnectionHelper(private val context: Context) {

    fun hasConnection(): Boolean {
        val cm = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkConnection(cm)
        } else {
            checkConnectionLegacy(cm)
        }
    }

    @Suppress("DEPRECATION")
    private fun checkConnectionLegacy(cm: ConnectivityManager) = try {
        var haveConnectedWifi = false
        var haveConnectedMobile = false
        val networkInfo = cm.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                if (networkInfo.isConnectedOrConnecting) {
                    haveConnectedWifi = true
                }
            }
            if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                if (networkInfo.isConnectedOrConnecting) {
                    haveConnectedMobile = true
                }
            }
        }

        haveConnectedWifi || haveConnectedMobile
    } catch (ex: Exception) {
        false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun checkConnection(cm: ConnectivityManager): Boolean {
        getNetworks(cm).forEach { network ->
            cm.getNetworkCapabilities(network)?.let {
                if (it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return true
                }
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getNetworks(cm: ConnectivityManager): List<Network> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.activeNetwork?.let { Collections.singletonList(it) } ?: emptyList()
        } else {
            cm.allNetworks.toList()
        }
    }
}
