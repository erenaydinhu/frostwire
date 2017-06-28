/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 *            Marcelina Knitter (@marcelinkaaa)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public final class HistoHashMap<K> {

    private final HashMap<K, Integer> map = new HashMap<>();

    // creates the comparator as a field to avoid GC pressure every
    // time histogram is called, but still not static (no need)
    private final Comparator<Entry<K, Integer>> cmp = new Comparator<Entry<K, Integer>>() {
        @Override
        public int compare(Entry<K, Integer> o1, Entry<K, Integer> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    };

    /**
     * (Cheap operation)
     *
     * @param key the key
     * @return the frequency
     */
    public int update(K key) {
        int r = 1;
        if (map.containsKey(key)) {
            r = 1 + map.get(key);
        }
        map.put(key, r);
        return r;
    }

    public Integer get(K key) {
        return map.get(key);
    }

    /**
     * (Expensive operation)
     * Returns sorted list of entries.
     *
     * @return the list
     */
    public List<Entry<K, Integer>> histogram() {
        try {
            ArrayList<Entry<K, Integer>> list = new ArrayList<>(map.entrySet());
            Collections.sort(list, cmp);
            return Collections.unmodifiableList(list);
        } catch (ConcurrentModificationException e) {
            // working with no synchronized structures, even with the copies
            // it's possible that this exception can happens, but the cost
            // of synchronization bigger than the lack of accuracy
            // ignore
            return Collections.emptyList();
        }
    }

    public int getKeyCount() {
        return map.size();
    }

    public void reset() {
        map.clear();
    }
}
