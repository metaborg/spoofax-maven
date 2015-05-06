package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.List;
import org.metaborg.spoofax.core.context.IContext;
import org.metaborg.spoofax.core.language.ILanguage;

public class ContextComparator implements Comparator<IContext>{
    
    private final List<ILanguage> languages;
    private final Ordering<Object> defaultOrdering;

    public ContextComparator(List<ILanguage> languages) {
        this.languages = languages;
        this.defaultOrdering = Ordering.arbitrary();
    }

    @Override
    public int compare(IContext o1, IContext o2) {
        int i1 = languages.indexOf(o1.language());
        int i2 = languages.indexOf(o2.language());
        return i1 >= 0 && i2 >= 0 ? i1 - i2 : defaultOrdering.compare(i1, i2);
    }

}
