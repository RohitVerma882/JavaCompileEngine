package com.xiaoyv.java.compiler

import android.app.Application
import android.util.Log
import com.xiaoyv.java.compiler.tools.dex.JavaDexCompiler
import com.xiaoyv.java.compiler.tools.exec.JavaProgram
import com.xiaoyv.java.compiler.tools.exec.JavaProgramConsole
import com.xiaoyv.java.compiler.tools.java.JavaClassCompiler
import com.xiaoyv.java.compiler.utils.GlobalUtils
import com.xiaoyv.java.compiler.utils.ResourceUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.PrintStream

/**
 * JavaEngine
 *
 * @author why
 * @since 2022/3/7
 */
object JavaEngine {
    private const val TAG = "JavaEngine"

    lateinit var stdSystemOut: PrintStream
    lateinit var stdSystemErr: PrintStream
    lateinit var stdSystemIn: InputStream

    internal var lastProgram: JavaProgramConsole? = null

    val CompileExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logError(throwable)
    }

    /**
     * 编译器设置
     */
    @JvmStatic
    val compilerSetting: JavaEngineSetting by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        JavaEngineSetting()
    }

    /**
     * Class 编译器
     */
    @JvmStatic
    val classCompiler: JavaClassCompiler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        JavaClassCompiler()
    }

    /**
     * Dex 编译器
     */
    @JvmStatic
    val dexCompiler: JavaDexCompiler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        JavaDexCompiler()
    }

    /**
     * Dex 执行器
     */
    @JvmStatic
    val javaProgram: JavaProgram by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        JavaProgram()
    }

    /**
     * 初始化
     */
    @JvmStatic
    fun init(application: Application) {
        GlobalUtils.init(application)

        CoroutineScope(Dispatchers.IO).launch {
            installRtJar().collect {
                Log.i(TAG, "Rt.jar 安装结果：$it")
            }
        }

        // 系统默认的 输出流|输入流
        stdSystemOut = System.out
        stdSystemErr = System.err
        stdSystemIn = System.`in`
    }

    /**
     * 安装 Rt.jar
     */
    private suspend fun installRtJar(): Flow<Boolean> = flow {
        val rtJar = File(compilerSetting.rtPath)

        // 不存在时从 Assets 复制
        val fileExists = rtJar.exists() && rtJar.length() != 0L
        if (fileExists) {
            emit(true)
        } else {
            emit(ResourceUtils.copyFileFromAssets("rt.jar", rtJar.absolutePath))
        }
    }

    /**
     * 恢复系统的默认标准流
     */
    fun resetStdStream() {
        System.setIn(stdSystemIn)
        System.setOut(stdSystemOut)
        System.setErr(stdSystemErr)
    }

    /**
     * 清空遗留的程序
     */
    fun resetProgram() {
        lastProgram?.close()
        lastProgram = null
    }

    internal fun logInfo(msg: String) {
        Log.i(TAG, msg)
    }

    internal fun logError(msg: String) {
        Log.e(TAG, msg)
    }

    internal fun logError(e: Throwable) {
        Log.e(TAG, "GlobalError: $e", e)
    }


}