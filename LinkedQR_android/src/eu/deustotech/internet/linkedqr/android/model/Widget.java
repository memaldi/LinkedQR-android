package eu.deustotech.internet.linkedqr.android.model;

/**
 * Created by mikel on 21/05/13.
 */
public class Widget {
    private String id;
    private boolean linkable;
    private boolean main;

    public Widget(String id, boolean linkable, boolean main) {
        this.id = id;
        this.linkable = linkable;
        this.main = main;
    }

    public Widget(String id) {
        this.id = id;
        this.linkable = false;
        this.main = false;
    }

    public boolean isLinkable() {
        return linkable;
    }

    public void setLinkable(boolean linkable) {
        this.linkable = linkable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }
}
