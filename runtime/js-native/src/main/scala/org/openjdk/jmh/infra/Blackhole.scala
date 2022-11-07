/*
 * Copyright 2022 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openjdk.jmh.infra

object Blackhole {

  @volatile
  private var consumedCPU: Long = System.nanoTime()

  def consumeCPU(tokens: Long): Unit = {
    var t = consumedCPU

    var i = tokens
    while (i > 0) {
      t += (t * 0x5deece66dL + 0xbL + i) & 0xffffffffffffL
      i -= 1
    }

    if (t == 42) {
      consumedCPU += t;
    }
  }

}
