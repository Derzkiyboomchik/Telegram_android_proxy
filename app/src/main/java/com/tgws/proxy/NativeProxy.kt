package com.tgws.proxy

import android.util.Log
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface ProxyLibrary : Library {
    companion object {
        val INSTANCE: ProxyLibrary by lazy {
            Native.load("tgwsproxy", ProxyLibrary::class.java) as ProxyLibrary
        }
    }

    fun StartProxy(host: String, port: Int, dcIps: String, secret: String, verbose: Int): Int
    fun StopProxy(): Int
    fun SetPoolSize(size: Int)
    fun SetNetworkOnline(online: Int)
    fun SetPowerSaveMode(enabled: Int)
    fun SetCfProxyCacheDir(cacheDir: String)
    fun SetCfProxyConfig(enabled: Int, priority: Int, userDomain: String)
    fun SetFakeTls(enabled: Int, domain: String)
    fun GetSecretWithPrefix(): Pointer?
    fun GetStats(): Pointer?
    fun FreeString(p: Pointer)
}

object NativeProxy {
    private const val TAG = "NativeProxy"
    private val lock = Any()

    fun startProxy(host: String, port: Int, dcIps: String, secret: String, verbose: Int): Int {
        return synchronized(lock) {
            try {
                ProxyLibrary.INSTANCE.StartProxy(host, port, dcIps, secret, verbose)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to call StartProxy", e)
                -1
            }
        }
    }

    fun stopProxy(): Int {
        return synchronized(lock) {
            try {
                ProxyLibrary.INSTANCE.StopProxy()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to call StopProxy", e)
                -1
            }
        }
    }

    fun setPoolSize(size: Int) {
        try {
            ProxyLibrary.INSTANCE.SetPoolSize(size)
        } catch (e: Throwable) {
            Log.w(TAG, "SetPoolSize not available in native library", e)
        }
    }

    fun setNetworkOnline(online: Boolean) {
        try {
            ProxyLibrary.INSTANCE.SetNetworkOnline(if (online) 1 else 0)
        } catch (e: Throwable) {
            Log.w(TAG, "SetNetworkOnline not available in native library", e)
        }
    }

    fun setPowerSaveMode(enabled: Boolean) {
        try {
            ProxyLibrary.INSTANCE.SetPowerSaveMode(if (enabled) 1 else 0)
        } catch (e: Throwable) {
            Log.w(TAG, "SetPowerSaveMode not available in native library", e)
        }
    }

    fun setCfProxyCacheDir(cacheDir: String) {
        try {
            ProxyLibrary.INSTANCE.SetCfProxyCacheDir(cacheDir)
        } catch (e: Throwable) {
            Log.w(TAG, "SetCfProxyCacheDir not available in native library", e)
        }
    }

    fun setCfProxyConfig(enabled: Boolean, priority: Boolean, userDomain: String) {
        try {
            ProxyLibrary.INSTANCE.SetCfProxyConfig(
                if (enabled) 1 else 0,
                if (priority) 1 else 0,
                userDomain
            )
        } catch (e: Throwable) {
            Log.w(TAG, "SetCfProxyConfig not available in native library", e)
        }
    }

    fun setFakeTls(enabled: Boolean, domain: String = "") {
        try {
            ProxyLibrary.INSTANCE.SetFakeTls(if (enabled) 1 else 0, domain)
        } catch (e: Throwable) {
            Log.w(TAG, "SetFakeTls not available in native library", e)
        }
    }

    /** Returns the full secret with correct prefix (dd or ee+domain_hex) */
    fun getSecretWithPrefix(): String? {
        return try {
            val ptr = ProxyLibrary.INSTANCE.GetSecretWithPrefix() ?: return null
            val res = ptr.getString(0)
            ProxyLibrary.INSTANCE.FreeString(ptr)
            res
        } catch (e: Throwable) {
            Log.w(TAG, "GetSecretWithPrefix error", e)
            null
        }
    }

    fun getStats(): String? {
        return try {
            val ptr = ProxyLibrary.INSTANCE.GetStats() ?: return null
            val res = ptr.getString(0)
            ProxyLibrary.INSTANCE.FreeString(ptr)
            res
        } catch (e: Throwable) {
            Log.w(TAG, "GetStats error", e)
            null
        }
    }
}
