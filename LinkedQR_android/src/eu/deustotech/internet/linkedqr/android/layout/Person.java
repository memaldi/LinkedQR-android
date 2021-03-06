package eu.deustotech.internet.linkedqr.android.layout;

import android.view.View;
import eu.deustotech.internet.linkedqr.android.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mikel on 17/05/13.
 */
public class Person implements Layout {
    @Override
    public int getLayout() {
        return R.layout.person;
    }

    @Override
    public Map<String, Integer> getWidgets() {
        Map<String, Integer> widgetMap = new HashMap<String, Integer>();
        widgetMap.put("name", R.id.name);
        widgetMap.put("email", R.id.email);
        widgetMap.put("workmates", R.id.workmates);
        widgetMap.put("depiction", R.id.depiction);
        widgetMap.put("social", R.id.social);

        return widgetMap;
    }
}
