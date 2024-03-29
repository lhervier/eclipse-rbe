/*
 * Copyright (C) 2003, 2004  Pascal Essiembre, Essiembre Consultant Inc.
 * 
 * This file is part of Essiembre ResourceBundle Editor.
 * 
 * Essiembre ResourceBundle Editor is free software; you can redistribute it 
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * Essiembre ResourceBundle Editor is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Essiembre ResourceBundle Editor; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
 * Boston, MA  02111-1307  USA
 */
package com.essiembre.eclipse.rbe.model.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.essiembre.eclipse.rbe.model.Model;


/**
 * Represents all properties files part of the same "family".
 * @author Pascal Essiembre (essiembre@users.sourceforge.net)
 * @version $Author: nl_carnage $ $Revision: 1.8 $ $Date: 2007/09/11 16:11:09 $
 */
public class BundleGroup extends Model implements IBundleVisitable {

    /** Bundles forming the group (key=Locale; value=Bundle). */
    private final Map<Locale, Bundle> bundles = new HashMap();
    
    //TODO tentative:
    private final SortedSet<String> keys = new TreeSet<String>();
//	private final List<String> keys = new LinkedList<String>();
    
    /**
     * Constructor.
     */
    public BundleGroup() {
        super();
    }

    /**
     * @see IBundleVisitable#accept(IBundleVisitor, Object)
     */
    public void accept(IBundleVisitor visitor, Object passAlongArgument) {
        for (Bundle bundle : bundles.values()) {
            for (Iterator j = bundle.iterator(); j.hasNext();) {
                visitor.visitBundleEntry(
                        (BundleEntry) j.next(), passAlongArgument);
            }
            visitor.visitBundle(bundle, passAlongArgument);
        }
        visitor.visitBundleGroup(this, passAlongArgument);
    }

    /**
     * Gets the number of bundles in this group.
     * @return the number of bundles in this group
     */
    public int getSize() {
        return bundles.size();
    }

    /**
     * Adds a bundle to this group.
     * @param locale bundle locale
     * @param bundle bundle to add
     */
    public void addBundle(Locale locale, Bundle bundle) {
        Bundle localBundle = (Bundle) bundles.get(locale);
        bundle.setLocale(locale);
        bundle.setBundleGroup(this);
        if (localBundle == null) {
            bundles.put(locale, bundle);
            refreshKeys();
            fireAdd(bundle);
        } else { // TODO if (!localBundle.equals(bundle)) {
            localBundle.copyFrom(bundle);
            refreshKeys();
            fireModify(bundle);
        }
    }

    /**
     * Gets the bundle matching given locale.
     * @param locale locale of bundle to retreive
     * @return a bundle
     */
    public Bundle getBundle(Locale locale) {
        return (Bundle) bundles.get(locale);
    }
    
    /**
     * Adds a bundle entry to the bundle in this group matching the given
     * locale.
     * @param locale locale of bundle we want to add an entry to
     * @param bundleEntry the entry to add
     */
    public void addBundleEntry(Locale locale, BundleEntry bundleEntry) {
        Bundle bundle = getBundle(locale);
        if (bundle != null) {
            BundleEntry existingEntry = 
                    getBundleEntry(locale, bundleEntry.getKey());
            if (!bundleEntry.equals(existingEntry)) {
                bundleEntry.setBundle(bundle);
                bundleEntry.setLocale(locale);
                bundle.addEntry(bundleEntry);
                refreshKeys();
                fireModify(bundle);
            }
        }
    }

    /**
     * Adds a key to this group by creating a bundle entry with the given
     * key and adding it to each bundle.
     * @param key
     */
    public void addKey(String key) {
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            BundleEntry entry = new BundleEntry(key, null, null);
            addBundleEntry(locale, entry);
        }
    }

    /**
     * Renames the key in each bundle entry matching the <code>oldKey</code>
     * to the <code>newKey</code>.
     * @param oldKey key to replace
     * @param newKey replacement key
     */
    public void renameKey(String oldKey, String newKey) {
        if (oldKey.equals(newKey)) {
            return;
        }
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            Bundle bundle = getBundle(locale);
            BundleEntry entry = getBundleEntry(locale, oldKey);
            if (entry != null) {
                bundle.renameKey(oldKey, newKey);
                refreshKeys();
                fireModify(bundle);
            }
            
        }
    }

    
    /**
     * Comment bundle entries matching the <code>key</code>.
     * @param key key to comment
     */
    public void commentKey(String key) {
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            Bundle bundle = getBundle(locale);
            BundleEntry entry = getBundleEntry(locale, key);
            if (entry != null) {
                bundle.commentKey(key);
                fireModify(bundle);
            }
        }
    }

    /**
     * Uncomment bundle entries matching the <code>key</code>.
     * @param key key to comment
     */
    public void uncommentKey(String key) {
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            Bundle bundle = getBundle(locale);
            BundleEntry entry = getBundleEntry(locale, key);
            if (entry != null) {
                bundle.uncommentKey(key);
                fireModify(bundle);
            }
        }
    }
    
    /**
     * Copies each bundle entries matching the <code>origKey</code> under
     * the <code>newKey</code>.
     * @param origKey original key
     * @param newKey new key
     */
    public void copyKey(String origKey, String newKey) {
        if (origKey.equals(newKey)) {
            return;
        }
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            Bundle bundle = getBundle(locale);
            BundleEntry origEntry = getBundleEntry(locale, origKey);
            if (origEntry != null) {
                bundle.copyKey(origKey, newKey);
                refreshKeys();
                fireModify(bundle);
            }
            
        }
    }

    /**
     * Removes bundle entries matching the given key in all bundles.
     * @param key key to remove
     */
    public void removeKey(String key) {
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            Bundle bundle = getBundle(locale);
            BundleEntry entry = getBundleEntry(locale, key);
            if (entry != null) {
                bundle.removeEntry(entry);
                refreshKeys();
                fireModify(bundle);
            }
        }
    }
    
    /**
     * Gets the bundle entry matching given key from the bundle matching the
     * given locale.
     * @param locale locale of bundle in which to find the entry
     * @param key key of bundle entry.
     * @return bundle entry
     */
    public BundleEntry getBundleEntry(Locale locale, String key) {
        Bundle bundle = getBundle(locale);
        if (bundle != null) {
            return bundle.getEntry(key);
        }
        return null;
    }
    
    /**
     * Gets bundle entries matching given key from all bundles.
     * @param key key of entries to retreive
     * @return a collection of <code>BundleEntry</code> objects
     */
    public Collection getBundleEntries(String key) {
        Collection entries = new ArrayList();
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            BundleEntry entry = getBundleEntry(locale, key);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }
    
    /**
     * Returns true if the supplied key is already existing in this group.
     * 
     * @param key   The key that shall be tested.
     * 
     * @return  true <=> The key is already existing.
     */
    public boolean containsKey(String key) {
        for (Iterator iter = bundles.keySet().iterator(); iter.hasNext();) {
            Locale locale = (Locale) iter.next();
            BundleEntry entry = getBundleEntry(locale, key);
            if (entry != null) {
                return (true);
            }
        }
        return (false);
    }
    
    /**
     * Returns the number of bundles currently registered with this group.
     * 
     * @return  The number of bundles currently registered with this group.
     */
    public int getBundleCount() {
        return (bundles.size());
    }

    /**
     * Iterates through all bundles in this group.
     * @return iterator.
     */
    public Iterator iterator() {
        return bundles.values().iterator();
    }

    /**
     * Gets all resource bundle keys.
     * @return <code>List</code> of resource bundle keys.
     */
    public SortedSet<String> getKeys() {
        return keys;
    }

    /**
     * Gets all resource bundle keys.
     * @return <code>List</code> of resource bundle keys.
     */
//    public Set getKeys() {
    private Set<String> refreshKeys() {
        //Set keys = new TreeSet();
        keys.clear();
        for (Iterator iter = iterator(); iter.hasNext();) {
            keys.addAll(((Bundle) iter.next()).getKeys());
        }
        return keys;
//        return Collections.unmodifiableSet(keys);
    }

    /**
     * Is the given key found in this bundle group.
     * @param key the key to find
     * @return <code>true</code> if the key exists in this bundle group.
     */
    public boolean isKey(String key) {
        return getKeys().contains(key);
    }
    
    public String getNextKey(String currentKey) {
        boolean returnNextKey = false;
        for (String key : keys) {
            if (returnNextKey)
                return key;
            if (key.equals(currentKey))
                returnNextKey = true;
        }
        return null;
//		int index = keys.indexOf(key);
//		if (index < keys.size()-1)
//			return keys.get(++index);
//		return null;
    }
    
    public String getPreviousKey(String currentKey) {
        String previousKey = null;
        for (String key : keys) {
            if (key.equals(currentKey))
                return previousKey;
            
            previousKey = key;
        }
        return null;
//		int index = keys.indexOf(key);
//		if (index > 0)
//			return keys.get(--index);
//		return null;
    }

}
