/*
 * Copyright (C) 2015 Artem Chepurnoy
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2009 The Android Open Source Project
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
package com.achep.base.utils.smiley;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.achep.acdisplay.R;
import com.achep.base.tests.Check;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public class SmileyParser {

    private static SmileyParser sInstance;

    @NonNull
    public static SmileyParser getInstance() {
        Check.getInstance().isNonNull(sInstance);
        return sInstance;
    }

    /**
     * Initializes the whole smileys parses thing and saves it into
     * a static reference. This method should be called from
     * {@link Application#onCreate()}.
     */
    public static void init(@NonNull Context context) {
        sInstance = new SmileyParser(context.getApplicationContext());
    }

    private final Context mContext;
    private final String[] mSmileyTexts;
    private final Pattern mPattern;
    private final HashMap<String, Integer> mSmileyToRes;

    private SmileyParser(@NonNull Context context) {
        mContext = context;
        mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
        mSmileyToRes = buildSmileyToRes();
        mPattern = buildPattern();
    }

    static class Smileys {
        private static final int[] sIconIds = {
                R.drawable.emo_im_happy,
                R.drawable.emo_im_sad,
                R.drawable.emo_im_winking,
                R.drawable.emo_im_tongue_sticking_out,
                R.drawable.emo_im_surprised,
                R.drawable.emo_im_kissing,
                R.drawable.emo_im_yelling,
                R.drawable.emo_im_cool,
                R.drawable.emo_im_money_mouth,
                R.drawable.emo_im_foot_in_mouth,
                R.drawable.emo_im_embarrassed,
                R.drawable.emo_im_angel,
                R.drawable.emo_im_undecided,
                R.drawable.emo_im_crying,
                R.drawable.emo_im_lips_are_sealed,
                R.drawable.emo_im_laughing,
                R.drawable.emo_im_wtf,
                R.drawable.emo_im_heart,
                R.drawable.emo_im_mad,
                R.drawable.emo_im_smirk,
                R.drawable.emo_im_pokerface
        };

        public static final int HAPPY = 0;
        public static final int SAD = 1;
        public static final int WINKING = 2;
        public static final int TONGUE_STICKING_OUT = 3;
        public static final int SURPRISED = 4;
        public static final int KISSING = 5;
        public static final int YELLING = 6;
        public static final int COOL = 7;
        public static final int MONEY_MOUTH = 8;
        public static final int FOOT_IN_MOUTH = 9;
        public static final int EMBARRASSED = 10;
        public static final int ANGEL = 11;
        public static final int UNDECIDED = 12;
        public static final int CRYING = 13;
        public static final int LIPS_ARE_SEALED = 14;
        public static final int LAUGHING = 15;
        public static final int WTF = 16;
        public static final int MAD = 17;
        public static final int HEART = 18;
        public static final int SMIRK = 19;
        public static final int POKERFACE = 20;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    // NOTE: if you change anything about this array, you must make the
    // corresponding change to the string arrays: default_smiley_texts
    // and default_smiley_names in res/values/cm_arrays.xml
    public static final int[] DEFAULT_SMILEY_RES_IDS = {
            Smileys.getSmileyResource(Smileys.HAPPY),                //  0
            Smileys.getSmileyResource(Smileys.SAD),                  //  1
            Smileys.getSmileyResource(Smileys.WINKING),              //  2
            Smileys.getSmileyResource(Smileys.TONGUE_STICKING_OUT),  //  3
            Smileys.getSmileyResource(Smileys.SURPRISED),            //  4
            Smileys.getSmileyResource(Smileys.KISSING),              //  5
            Smileys.getSmileyResource(Smileys.YELLING),              //  6
            Smileys.getSmileyResource(Smileys.COOL),                 //  7
            Smileys.getSmileyResource(Smileys.MONEY_MOUTH),          //  8
            Smileys.getSmileyResource(Smileys.FOOT_IN_MOUTH),        //  9
            Smileys.getSmileyResource(Smileys.EMBARRASSED),          //  10
            Smileys.getSmileyResource(Smileys.ANGEL),                //  11
            Smileys.getSmileyResource(Smileys.UNDECIDED),            //  12
            Smileys.getSmileyResource(Smileys.CRYING),               //  13
            Smileys.getSmileyResource(Smileys.LIPS_ARE_SEALED),      //  14
            Smileys.getSmileyResource(Smileys.LAUGHING),             //  15
            Smileys.getSmileyResource(Smileys.WTF),                  //  16
            Smileys.getSmileyResource(Smileys.MAD),                  //  17
            Smileys.getSmileyResource(Smileys.HEART),                //  18
            Smileys.getSmileyResource(Smileys.SMIRK),                //  19
            Smileys.getSmileyResource(Smileys.POKERFACE),            //  20
    };

    public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;

    /**
     * Builds the hashtable we use for mapping the string version
     * of a smiley (e.g. ":-)") to a resource ID for the icon version.
     */
    @NonNull
    private HashMap<String, Integer> buildSmileyToRes() {
        if (DEFAULT_SMILEY_RES_IDS.length != mSmileyTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<>(mSmileyTexts.length);
        for (int i = 0; i < mSmileyTexts.length; i++) {
            smileyToRes.put(mSmileyTexts[i], DEFAULT_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    /**
     * Builds the regular expression we use to find smileys in { @link #addSmileySpans }.
     */
    @NonNull
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : mSmileyTexts) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }

        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such
     * as :-) with a graphical version.
     *
     * @param text A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any
     * recognized emoticons.
     */
    public CharSequence addSmileySpans(@Nullable CharSequence text) {
        if (text == null) return null;

        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = mSmileyToRes.get(matcher.group());
            int start = matcher.start();
            int end = matcher.end();
            Check.getInstance().isTrue(end > start);
            builder.setSpan(new ImageSpan(mContext, resId), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}