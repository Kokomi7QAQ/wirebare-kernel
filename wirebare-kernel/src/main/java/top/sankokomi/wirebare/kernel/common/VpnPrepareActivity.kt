/*
 * MIT License
 *
 * Copyright (c) 2025 KokomiQAQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package top.sankokomi.wirebare.kernel.common

import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

open class VpnPrepareActivity : ComponentActivity() {

    private val resultCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                onPrepareSuccess()
            } else {
                onPrepareFail()
            }
        }

    open fun onPrepareSuccess() {
    }

    open fun onPrepareFail() {
    }

    /**
     * 准备 VPN 服务，若准备成功（用户授权），则回调 [onPrepareSuccess] 否则回调 [onPrepareFail]
     * */
    fun prepareProxy() {
        val intent = VpnService.prepare(this) ?: return onPrepareSuccess()
        this.resultCallback.launch(intent)
    }

}