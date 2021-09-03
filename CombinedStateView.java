package ext.deere.mcad.mbdviewer.domain;

import com.ptc.pview.pvkapp.ViewStateSource;

import java.util.Objects;

/**
 * Class that represents a Combined State View in Creo, may also be used to hold the current view, if that is true the 
 * name will be set to "current".
 */
public class CombinedStateView extends ViewState {
    private String savedImagePath = "";

    public CombinedStateView(String name) {
        super(name);
    }

    public CombinedStateView(String name, String savedImagePath) {
        super(name);
        this.savedImagePath = savedImagePath;
    }

    public CombinedStateView(String name, ViewStateSource viewStateSource) {
        super(name, viewStateSource);
    }


    public String getSavedImagePath() {
        return savedImagePath;
    }

    public void setSavedImagePath(String savedImagePath) {
        this.savedImagePath = savedImagePath;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CombinedStateView))
            return false;

        return ((CombinedStateView) o).getName().equals(this.getName()) 
                && ((CombinedStateView) o).getSavedImagePath().equals(this.getSavedImagePath()) 
                && Objects.equals(((CombinedStateView) o).getCreoViewViewStateSource(), (this.getCreoViewViewStateSource()));
    }
}
