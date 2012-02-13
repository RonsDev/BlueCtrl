/*
 * Copyright (C) 2012
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

package org.ronsdev.bluectrl;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Describes the characters that can be produced with the Keyboard and the associated HID Reports
 * to produce them.
 */
public class CharKeyReportMap {

    /**
     * Class that contains informations for a HID Keyboard Report sequence to produce a character.
     */
    public final class KeyReport {

        private int mModifier;
        private int mKeyCode;

        public KeyReport(int modifier, int keyCode) {
            mModifier = modifier;
            mKeyCode = keyCode;
        }

        public int getModifier() {
            return mModifier;
        }

        public int getKeyCode() {
            return mKeyCode;
        }
    }

    /**
     * Class that contains a sequence of HID Keyboard Reports to produce a character.
     */
    public final class KeyReportSequence extends ArrayList<KeyReport> {

        private static final long serialVersionUID = 5492730235502340972L;
    }


    private static final String TAG = "CharKeyReportMap";

    private static final String KEYMAPS_PATH = "keymaps/";


    private HashMap<Character, KeyReportSequence> mInternalMap;


    public CharKeyReportMap(String fileName, AssetManager assetManager) {
        mInternalMap = new HashMap<Character, KeyReportSequence>();

        loadKeyMapFile(fileName, assetManager);
    }

    private void add(Character key, int modifier, int keyCode) {
        KeyReportSequence sequence = new KeyReportSequence();
        sequence.add(new KeyReport(modifier, keyCode));
        add(key, sequence);
    }

    private void add(Character key, KeyReportSequence sequence) {
        if (!mInternalMap.containsKey(key)) {
            mInternalMap.put(key, sequence);
        }
    }

    private void parseKeyMapRow(String row) {
        String[] cells = row.split("\\t");
        if (cells.length < 3) {
            return;
        }

        Character keychar = cells[0].charAt(0);
        if (mInternalMap.containsKey(keychar)) {
            Log.w(TAG, String.format("redundant HidKeyMap char '%c'", keychar));
            return;
        }

        KeyReportSequence keyReportSequence = new KeyReportSequence();
        for (int i = 2; i < cells.length; i += 2) {
            try {
                final int modifier = Integer.parseInt(cells[i - 1]);
                final int keyCode = Integer.parseInt(cells[i]);
                keyReportSequence.add(new KeyReport(modifier, keyCode));
            } catch (NumberFormatException e) {
                Log.e(TAG, "invalid HidKeyMap number", e);
                return;
            }
        }

        add(keychar, keyReportSequence);
    }

    private void loadKeyMapFile(String fileName, AssetManager assetManager) {
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader reader = null;
        try {
            inputStream = assetManager.open(KEYMAPS_PATH + fileName);
            try {
                inputReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputReader);

                String row;
                while ((row = reader.readLine()) != null) {
                    final int commentIndex = row.indexOf("//");
                    if (commentIndex > -1) {
                        row = row.substring(0, commentIndex);
                    }

                    parseKeyMapRow(row);
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (inputReader != null) {
                    inputReader.close();
                }
                inputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, String.format("read HidKeyMap '%s' failed", fileName), e);
        }

        // Add Whitespace characters
        add(' ', 0, 44);
        add('\n', 0, 40);
        add('\t', 0, 43);
    }

    public KeyReportSequence get(char key) {
        if (mInternalMap.containsKey(key)) {
            return mInternalMap.get(key);
        } else {
            return null;
        }
    }
}
