package com.odobo.grails.plugin.springsecurity.rest

import com.odobo.grails.plugin.springsecurity.rest.token.reader.TokenReader
import com.odobo.grails.plugin.springsecurity.rest.token.storage.TokenNotFoundException
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import spock.lang.Specification

class RestTokenValidationFilterUnitSpec extends Specification {

    def filter = new RestTokenValidationFilter(active: true)

    def request  = new GrailsMockHttpServletRequest()
    def response = new MockHttpServletResponse()
    def chain = new MockFilterChain()

    def setup() {
        filter.authenticationSuccessHandler = Mock(AuthenticationSuccessHandler)
        filter.authenticationFailureHandler = Mock(AuthenticationFailureHandler)
        filter.tokenReader = Mock(TokenReader)
        filter.eventPublisher = Mock(ApplicationEventPublisher)
    }

    void "authentication passes when a valid token is found"() {
        given:
        filter.restAuthenticationProvider = new StubRestAuthenticationProvider(
                validToken: token,
                username: 'user',
                password: 'password'
        )


        when:
        filter.doFilter( request, response, chain )

        then:
        response.status == 200
        1 * filter.tokenReader.findToken(request, response) >> token
        0 * filter.authenticationFailureHandler.onAuthenticationFailure( _, _, _ )
        1 * filter.eventPublisher.publishEvent(_ as InteractiveAuthenticationSuccessEvent)
        notThrown( TokenNotFoundException )

        where:
        token = 'mytokenvalue'
    }

    void "authentication fails when a token cannot be found"() {
        given:
        filter.restAuthenticationProvider = new StubRestAuthenticationProvider(
                validToken: token,
                username: 'user',
                password: 'password'
        )

        when:
        filter.doFilter( request, response, chain )

        then:
        1 * filter.tokenReader.findToken(request, response) >> null
        1 * filter.authenticationFailureHandler.onAuthenticationFailure( request, response, _ as AuthenticationException )

        where:
        token = 'mytokenvalue'
    }
}

/**
 * Stubs out the RestAuthenticationProvider so that we can specify what a valid token is,
 * and have it return as if it authentication correctly if the token matches.
 */
class StubRestAuthenticationProvider extends RestAuthenticationProvider {

    String validToken
    String username
    String password

    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        authentication = authentication as RestAuthenticationToken
        if( authentication.tokenValue == validToken ) {
            return new RestAuthenticationToken( username, password, null, validToken )
        }

        throw new TokenNotFoundException( 'Token not found' )
    }
}
