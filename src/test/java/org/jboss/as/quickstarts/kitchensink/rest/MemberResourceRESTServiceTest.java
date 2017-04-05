package org.jboss.as.quickstarts.kitchensink.rest;

import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.jglue.cdiunit.CdiRunner;
import org.jglue.cdiunit.InRequestScope;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(CdiRunner.class)
public class MemberResourceRESTServiceTest {

    @Inject
    MemberResourceRESTService restService;

    @Produces
    @Mock
    MemberRepository repository;

    @Produces
    @Mock
    MemberRegistration registration;

    @Produces
    public Logger produceLog(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
    }

    @Produces
    public Validator produceValidator() {
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        return vf.getValidator();
    }

    @Test
    @InRequestScope
    public void testLookupMemberById() {
        Member member = new Member();
        member.setId(1L);
        member.setName("John Doe");
        member.setEmail("john@doe.com");
        member.setPhoneNumber("1234567890");
        Mockito.when(repository.findById(1L)).thenReturn(member);
        Member lookedUp = restService.lookupMemberById(1);
        Assert.assertEquals(member, lookedUp);
        Mockito.verify(repository).findById(1L);
    }

    @Test
    @InRequestScope
    public void testLookupMemberByIdReturnsNull() {
        Mockito.when(repository.findById(1L)).thenReturn(null);
        try {
            restService.lookupMemberById(1);
            Assert.fail();
        } catch (WebApplicationException wae) {
        }
        Mockito.verify(repository).findById(1L);
    }

    @Test
    @InRequestScope
    public void testCreateMember() throws Exception {
        Member member = new Member();
        member.setId(1L);
        member.setName("John Doe");
        member.setEmail("john@doe.com");
        member.setPhoneNumber("1234567890");
        Response response = restService.createMember(member);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Mockito.verify(registration).register(member);
    }

    @SuppressWarnings("unchecked")
    @Test
    @InRequestScope
    public void testCreateMemberNullEmail() throws Exception {
        Member member = new Member();
        member.setId(1L);
        member.setName("John Doe");
        member.setEmail(null);
        member.setPhoneNumber("1234567890");
        Response response = restService.createMember(member);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        Assert.assertEquals(1, entity.size());
        Assert.assertTrue(entity.containsKey("email"));
        Mockito.verify(registration, Mockito.never()).register(member);
    }

    @SuppressWarnings("unchecked")
    @Test
    @InRequestScope
    public void testCreateMemberEmailTaken() throws Exception {
        Member member = new Member();
        member.setId(1L);
        member.setName("John Doe");
        member.setEmail("john@email.com");
        member.setPhoneNumber("1234567890");
        Mockito.when(repository.findByEmail("john@email.com")).thenReturn(new Member());
        Response response = restService.createMember(member);
        Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        Assert.assertEquals(1, entity.size());
        Assert.assertTrue(entity.containsKey("email"));
        Mockito.verify(registration, Mockito.never()).register(member);
    }

    @SuppressWarnings("unchecked")
    @Test
    @InRequestScope
    public void testCreateMemberException() throws Exception {
        Member member = new Member();
        member.setId(1L);
        member.setName("John Doe");
        member.setEmail("john@email.com");
        member.setPhoneNumber("1234567890");
        Mockito.doThrow(new IllegalStateException("Error")).when(registration).register(member);
        Response response = restService.createMember(member);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        Assert.assertEquals(1, entity.size());
        Assert.assertTrue(entity.containsKey("error"));
        Mockito.verify(registration).register(member);
    }

}
