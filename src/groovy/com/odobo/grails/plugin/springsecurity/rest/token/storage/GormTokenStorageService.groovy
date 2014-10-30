package com.odobo.grails.plugin.springsecurity.rest.token.storage

import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.security.core.userdetails.UserDetailsService

/**
 * CUSTOM GORM implementation for token storage. It will look for tokens on the DB using a domain class that will contain the
 * generated token and the username associated.
 *
 * Once the username is found, it will delegate to the configured {@link UserDetailsService} for obtaining authorities
 * information.
 */
class GormTokenStorageService implements TokenStorageService, GrailsApplicationAware {

    /** Dependency injection for the application. */
    GrailsApplication grailsApplication

    UserDetailsService userDetailsService

    Object loadUserByToken(String tokenValue) throws TokenNotFoundException {
        def existingToken = findExistingToken(tokenValue)
        if (existingToken) {
            return userDetailsService.loadUserByUsername(existingToken.username, existingToken.vendorKey, existingToken.supportUsername)
        }
        throw new TokenNotFoundException("Token ${tokenValue.mask()} not found")
    }

    void storeToken(String tokenValue, Object principal) {
        def conf = SpringSecurityUtils.securityConfig
        String tokenClassName = conf.rest.token.storage.gorm.tokenDomainClassName
        def dc = grailsApplication.getClassForName(tokenClassName)
        dc.withTransaction { status ->
            def newTokenObject = dc.newInstance(tokenValue: tokenValue, username: principal.username)
            newTokenObject.save()
        }
    }

    Object storeMarkedToken(String tokenValue, Object principal) {
        def conf = SpringSecurityUtils.securityConfig
        String tokenClassName = conf.rest.token.storage.gorm.tokenDomainClassName
        def dc = grailsApplication.getClassForName(tokenClassName)
        dc.withTransaction { status ->
            def newTokenObject = dc.newInstance(tokenValue: tokenValue, username: principal.username, vendorKey:principal.vendorKey, switchUsername:principal.switchUsername)
            newTokenObject.save()
            return newTokenObject
        }
    }

    void removeToken(String tokenValue) throws TokenNotFoundException {
        def existingToken = findExistingToken(tokenValue)
        if (existingToken) {
            existingToken.delete()
        } else {
            throw new TokenNotFoundException("Token ${tokenValue.mask()} not found")
        }
    }

    private findExistingToken(String tokenValue) {
        def conf = SpringSecurityUtils.securityConfig
        String tokenClassName = conf.rest.token.storage.gorm.tokenDomainClassName
        def dc = grailsApplication.getClassForName(tokenClassName)
        dc.withTransaction { status ->
            return dc.findByTokenValue(tokenValue, [readOnly:true])
        }
    }

}
