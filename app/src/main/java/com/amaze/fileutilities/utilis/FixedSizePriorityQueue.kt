/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.utilis

import java.util.PriorityQueue

/**
 * A [PriorityQueue] that will at most only contain [fixedSize] many elements.
 * If more elements than [fixedSize] are added, the smallest elements based on [comparator] will be removed.
 * Therefore, this priority queue will only contain the largest elements that were added to it.
 */
class FixedSizePriorityQueue<E>(
    private val fixedSize: Int,
    comparator: Comparator<E>
) : PriorityQueue<E>(fixedSize + 1, comparator) {
    // initial capacity is set to fixedSize + 1 because we first add the new element and then fix the size
    /**
     * Adds [element] to the priority queue.
     * If there are already [fixedSize] many elements in the queue then the smallest element is removed.
     */
    override fun add(element: E): Boolean {
        super.add(element)
        // Makes sure that the size of the priority queue doesn't exceed fixedSize
        if (this.size > fixedSize) {
            this.remove()
        }
        return true
    }
}
