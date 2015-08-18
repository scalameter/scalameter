package org.scalameter.deprecatedjapi;



import java.util.*;



public class JContext implements java.io.Serializable {
	private Map<String, Object> keymap;

	public JContext(Map<String, Object> m) {
		keymap = m;
	}

	public static JContext create() {
		return new JContext(new HashMap<String, Object>());
	}

	public JContext put(String key, Object value) {
		HashMap<String, Object> nm = new HashMap<String, Object>();
		for (Map.Entry<String, Object> e: keymap.entrySet()) {
			nm.put(e.getKey(), e.getValue());
		}
		nm.put(key, value);
		return new JContext(nm);
	}

	public Map<String, Object> getKeyMap() {
		return keymap;
	}
}

