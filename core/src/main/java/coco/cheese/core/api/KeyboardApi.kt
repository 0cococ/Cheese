package coco.cheese.core.api

import coco.cheese.core.Env
import coco.cheese.core.aidl.client.IKeyboardClient
import coco.cheese.core.engine.javet.Promise
import coco.cheese.core.interfaces.IBase
import coco.cheese.core.interfaces.IEngineBase
import coco.cheese.core.utils.KeyboardUtils
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.values.reference.V8ValuePromise
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService

class KeyboardApi (private val env: Env): IEngineBase {
    override val executorService: ExecutorService = env.executorService
    override lateinit var nodeRuntime: NodeRuntime
    @V8Function
    override fun setNodeRuntime(key: String) {
        this.nodeRuntime = env.nodeRuntime[key]!!.nodeRuntime
    }
    @V8Function
    fun inputAsync(text: String): V8ValuePromise {
        val v8ValuePromiseResolver = nodeRuntime.createV8ValuePromise()
        val task: Promise.Task =
            Promise.Task(v8ValuePromiseResolver, "", System.currentTimeMillis())
        executorService.submit {
            Promise(nodeRuntime, task,
                env.invoke<IKeyboardClient>().input(text)
            )
        }
        return v8ValuePromiseResolver.promise
    }
    @V8Function
    fun input(text: String) {
        env.invoke<IKeyboardClient>().input(text)
    }

    @V8Function
    fun deleteAsync(): V8ValuePromise {
        val v8ValuePromiseResolver = nodeRuntime.createV8ValuePromise()
        val task: Promise.Task =
            Promise.Task(v8ValuePromiseResolver, "", System.currentTimeMillis())
        executorService.submit {
            Promise(nodeRuntime, task,
                env.invoke<IKeyboardClient>().delete()
            )
        }
        return v8ValuePromiseResolver.promise
    }
    @V8Function
    fun delete() {
        env.invoke<IKeyboardClient>().delete()
    }

    @V8Function
    fun enterAsync(): V8ValuePromise {
        val v8ValuePromiseResolver = nodeRuntime.createV8ValuePromise()
        val task: Promise.Task =
            Promise.Task(v8ValuePromiseResolver, "", System.currentTimeMillis())
        executorService.submit {
            Promise(nodeRuntime, task,
                env.invoke<IKeyboardClient>().enter()
            )
        }
        return v8ValuePromiseResolver.promise
    }
    @V8Function
    fun enter() {
        env.invoke<IKeyboardClient>().enter()
    }

    companion object : IBase {
        private var instanceWeak: WeakReference<KeyboardApi>? = null
        private var instance: KeyboardApi? = null
        private val lock = Any()
        override fun get(env: Env, examine: Boolean) : KeyboardApi {
            if (this.instance == null || !examine) {
                synchronized(this.lock) {
                    this.instance = KeyboardApi(env)
                }
            }
            return this.instance!!
        }

        override fun getWeak(env: Env, examine: Boolean): KeyboardApi {
            if (this.instanceWeak?.get() == null || !examine) {
                synchronized(this.lock) {
                    this.instanceWeak = WeakReference(KeyboardApi(env))
                }
            }
            return this.instanceWeak?.get()!!
        }

    }
}