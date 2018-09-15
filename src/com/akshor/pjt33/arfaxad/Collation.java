package com.akshor.pjt33.arfaxad;

import java.text.*;

class Collation
{
	/**
	 * Our own default rules are much simpler than Java's. Just ignore all control characters except tab, and all zero-width characters.
	 */
	private static final String CONTROL_CHARS =
		"='\u0000'='\u0001'='\u0002'='\u0003'='\u0004'='\u0005'='\u0006'='\u0007'" +
		"='\b'='\n'='\u000b'='\f'='\r'='\u000e'='\u000f'='\u0010'" +
		"='\u0011'='\u0012'='\u0013'='\u0014'='\u0015'='\u0016'='\u0017'='\u0018'" +
		"='\u0019'='\u001a'='\u001b'='\u001c'='\u001d'='\u001e'='\u001f'='\u007f'" +
		"='\u0080'='\u0081'='\u0082'='\u0083'='\u0084'='\u0085'='\u0086'='\u0087'" +
		"='\u0088'='\u0089'='\u008a'='\u008b'='\u008c'='\u008d'='\u008e'='\u008f'" +
		"='\u0090'='\u0091'='\u0092'='\u0093'='\u0094'='\u0095'='\u0096'='\u0097'" +
		"='\u0098'='\u0099'='\u009a'='\u009b'='\u009c'='\u009d'='\u009e'='\u009f'" +
		"='\u200b'='\u200c'='\u200d'='\u200e'='\u200f'='\ufeff'";

	private static Collator activeCollator = Collator.getInstance();

	static Collator active() {
		return activeCollator;
	}

	static void reload() {
		// Java's default collation rules are, frankly, bonkers.
		// For example, there's no obvious reason for apparently including Icelandic letters or Cyrillic accents in the default rules which apply to all locales.
		String rulesOverride = Arfaxad.resources.getString("collation.rules");
		try {
			activeCollator = rulesOverride == null ? Collator.getInstance() : new RuleBasedCollator(CONTROL_CHARS + rulesOverride);
		}
		catch (ParseException pex) {
			System.err.println(pex);
			activeCollator = Collator.getInstance();
		}

		activeCollator.setStrength(Collator.PRIMARY);
	}
}