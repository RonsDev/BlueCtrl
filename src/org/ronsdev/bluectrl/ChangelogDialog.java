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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Dialog that shows the app changelog.
 */
public class ChangelogDialog {

    private static final String CHANGELOG_ASSETS_FILE = "CHANGELOG.md";

    private static final String HEADER_LINE_REGEX = "^--*$";


    private static CharSequence getHtmlText(Context context) {
        StringBuilder result = new StringBuilder();

        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader reader = null;
        try {
            inputStream = context.getAssets().open(CHANGELOG_ASSETS_FILE);
            try {
                inputReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputReader);

                String currentLine = reader.readLine();
                String nextLine = reader.readLine();
                while (nextLine != null) {
                    result.append(parseLine(currentLine, nextLine)).append('\n');

                    currentLine = nextLine;
                    nextLine = reader.readLine();
                }
                result.append(parseLine(currentLine, null));
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
            e.printStackTrace();
        }

        return Html.fromHtml(result.toString());
    }

    private static String parseLine(String currentLine, String nextLine) {
        if ((nextLine != null) && nextLine.matches(HEADER_LINE_REGEX)) {
            return String.format("<h2>%s</h2>", currentLine);
        } else if (currentLine.matches(HEADER_LINE_REGEX)) {
            return "";
        } else if (currentLine.startsWith("*")) {
            return String.format("\u2022%s<br>", currentLine.substring(1));
        } else {
            return String.format("%s<br>", currentLine);
        }
    }

    public static Dialog createDialog(Activity activity) {
        LayoutInflater inflater =
                (LayoutInflater)activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.changelog_dialog,
                (ViewGroup)activity.findViewById(R.id.view_changelog));

        TextView changelogText = (TextView)layout.findViewById(R.id.changelog_text);
        changelogText.setText(getHtmlText(activity));

        return new AlertDialog.Builder(activity)
            .setTitle(R.string.changelog)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .create();
    }
}
