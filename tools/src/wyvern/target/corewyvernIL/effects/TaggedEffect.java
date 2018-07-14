package wyvern.target.corewyvernIL.effects;

import wyvern.target.corewyvernIL.expression.Path;
import wyvern.tools.errors.FileLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TaggedEffect extends Effect {
    private final List<String> tag;

    public static TaggedEffect fromEffect(Effect e, ArrayDeque<String> breadcrumbs) {
        return new TaggedEffect(e.getPath(), e.getName(), e.getLocation(), breadcrumbs);
    }

    private TaggedEffect(Path p, String n, FileLocation l, ArrayDeque<String> breadcrumbs) {
        super(p, n, l);
        this.tag = new ArrayList<>(breadcrumbs);
        Collections.reverse(this.tag);
    }

    public List<String> getTag() {
        return this.tag;
    }

    public String prettyString() {
        if (this.tag.size() == 0) {
            return this.getName();
        }
        StringBuilder sb = new StringBuilder(this.tag.get(0));
        for (int i = 1; i < this.tag.size(); i++) {
            sb.append(".");
            sb.append(this.tag.get(i));
        }
        sb.append(".");
        sb.append(this.getName());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "<" + this.tag + ">" + super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TaggedEffect)) {
            return false;
        }

        TaggedEffect teObj = (TaggedEffect) obj;
        return teObj.getName().equals(getName())
                && teObj.getPath().equals(getPath())
                && teObj.getTag().equals(getTag());
    }

    @Override
    public int hashCode() {
        final int prime = 67;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getPath() == null) ? 0 : getPath().hashCode());
        result = prime * result + ((getPath() == null) ? 0 : getTag().hashCode());
        return result;
    }
}
