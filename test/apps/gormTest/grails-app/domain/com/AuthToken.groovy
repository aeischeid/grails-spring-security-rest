package com

class AuthToken {
	String tokenValue
	String username
	//Date refreshed = new Date()  // intended to work with afterLoad example below

	static constraints = {
		// helps ensure a ridgid and precise schema at the DB level
		tokenValue(nullable:false, maxSize:32)
		username(nullable:false)
	}

	static mapping = {
		version(false) // Grails' default optimistic locking mechanism is unwanted on this domain
		id(generator:'assigned', name: 'tokenValue') // tokenValue is unique & is way a token will be looked up so using it as the 'id/pkey' for makes sense
		cache(true) // Grails by default comes with ehcache configured. Take advantage of that on repeat token lookups.
	}

	/*
		NOTE: this is an example approach for keeping a token fresh without to frequently doing DB updates
		It would most likely be paired with a Quartz job to remove stale tokens at some interval
	
	def afterLoad() {
		// if being accessed and it is more than a day since last marked as refreshed
		// and it hasn't been wiped out by Quartz job (it exists, duh)
		// then refresh it
		if (refreshed < new Date() -1) {
			refreshed = new Date()
			this.save()
		}
	}
	*/
}
