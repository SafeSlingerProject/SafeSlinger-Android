package edu.cmu.cylab.starslinger.util;

import android.content.SearchRecentSuggestionsProvider;

public class CustomSuggestionsProvider extends SearchRecentSuggestionsProvider{
    
    public final static String AUTHORITY = "edu.cmu.cylab.starslinger.util.CustomSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;
    
    public CustomSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
