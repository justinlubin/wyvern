package wyvern.target.corewyvernIL.effects;

import wyvern.target.corewyvernIL.expression.Path;
import wyvern.target.corewyvernIL.expression.Variable;
import wyvern.tools.errors.FileLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class QualifiedEffect extends Effect {
    private final List<String> qualifier;

    private static boolean isReal(String breadcrumb) {
        return !breadcrumb.matches("var_[0-9]+");
    }

    public static QualifiedEffect fromEffect(Effect e, ArrayDeque<String> breadcrumbs) {
        return new QualifiedEffect(e.getPath(), e.getName(), e.getLocation(), breadcrumbs);
    }

    private QualifiedEffect(Path p, String n, FileLocation l, ArrayDeque<String> breadcrumbs) {
        super(p, n, l);
        this.qualifier = new ArrayList<>();
        for (Iterator<String> iter = breadcrumbs.descendingIterator(); iter.hasNext();) {
            String breadcrumb = iter.next();
            if (isReal(breadcrumb)) {
                this.qualifier.add(breadcrumb);
            }
        }
        if (p instanceof Variable) {
            String breadcrumb = ((Variable) p).getName();
            if (isReal(breadcrumb)) {
                this.qualifier.add(breadcrumb);
            }
        }
    }

    public List<String> getQualifier() {
        return this.qualifier;
    }

    public String prettyString() {
        if (this.qualifier.size() == 0) {
            return this.getName();
        }
        StringBuilder sb = new StringBuilder(this.qualifier.get(0));
        for (int i = 1; i < this.qualifier.size(); i++) {
            sb.append(".");
            sb.append(this.qualifier.get(i));
        }
        sb.append(".");
        sb.append(this.getName());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "<" + this.qualifier + ">" + super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof QualifiedEffect)) {
            return false;
        }

        QualifiedEffect teObj = (QualifiedEffect) obj;
        return teObj.getName().equals(getName())
                && (teObj.getPath() == null && getPath() == null || teObj.getPath().equals(getPath()))
                && teObj.getQualifier().equals(getQualifier());
    }

    @Override
    public int hashCode() {
        final int prime = 67;
        int result = 1;
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getPath() == null) ? 0 : getPath().hashCode());
        result = prime * result + ((getPath() == null) ? 0 : getQualifier().hashCode());
        return result;
    }
}
