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

package top.sankokomi.wirebare.kernel.dashboard

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.sankokomi.wirebare.kernel.annotation.Experimental
import top.sankokomi.wirebare.kernel.common.DynamicConfiguration

@Experimental
object WireBareDashboard {

    internal val mutableBandwidthFlow = MutableSharedFlow<Bandwidth>(10, 0, BufferOverflow.SUSPEND)

    /**
     * 带宽，可以在 [DynamicConfiguration.bandwidthStatInterval] 中设置回调频率
     * */
    @Experimental
    val bandwidthFlow: SharedFlow<Bandwidth> = mutableBandwidthFlow.asSharedFlow()

    internal val mutableReqBandwidthFlow = MutableSharedFlow<Bandwidth>(10, 0, BufferOverflow.SUSPEND)

    /**
     * 请求带宽，可以在 [DynamicConfiguration.bandwidthStatInterval] 中设置回调频率
     * */
    @Experimental
    val reqBandwidthFlow: SharedFlow<Bandwidth> = mutableReqBandwidthFlow.asSharedFlow()

    internal val mutableRspBandwidthFlow = MutableSharedFlow<Bandwidth>(10, 0, BufferOverflow.SUSPEND)

    /**
     * 响应带宽，可以在 [DynamicConfiguration.bandwidthStatInterval] 中设置回调频率
     * */
    @Experimental
    val rspBandwidthFlow: SharedFlow<Bandwidth> = mutableRspBandwidthFlow.asSharedFlow()
}