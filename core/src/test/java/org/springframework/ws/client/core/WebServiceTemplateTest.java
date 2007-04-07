/*
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.client.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.custommonkey.xmlunit.XMLTestCase;
import org.easymock.MockControl;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.MockWebServiceMessageFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.transport.FaultAwareWebServiceConnection;
import org.springframework.ws.transport.MockTransportInputStream;
import org.springframework.ws.transport.MockTransportOutputStream;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

public class WebServiceTemplateTest extends XMLTestCase {

    private WebServiceTemplate template;

    private MockControl connectionControl;

    private FaultAwareWebServiceConnection connectionMock;

    protected void setUp() throws Exception {
        template = new WebServiceTemplate();
        MockWebServiceMessageFactory messageFactory = new MockWebServiceMessageFactory();
        template.setMessageFactory(messageFactory);
        connectionControl = MockControl.createStrictControl(FaultAwareWebServiceConnection.class);
        connectionMock = (FaultAwareWebServiceConnection) connectionControl.getMock();
        template.setMessageSender(new WebServiceMessageSender() {

            public WebServiceConnection createConnection() throws IOException {
                return connectionMock;
            }
        });

    }

    public void testMarshalAndSendNoMarshallerSet() throws Exception {
        template.setMarshaller(null);
        try {
            template.marshalSendAndReceive(new Object());
            fail("IllegalStateException expected");
        }
        catch (IllegalStateException ex) {
            // expected behavior
        }
    }

    public void testMarshalAndSendNoUnmarshallerSet() throws Exception {
        template.setUnmarshaller(null);
        try {
            template.marshalSendAndReceive(new Object());
            fail("IllegalStateException expected");
        }
        catch (IllegalStateException ex) {
            // expected behavior
        }
    }

    public void testSendAndReceiveMessageResponse() throws Exception {
        MockControl callbackControl = MockControl.createControl(WebServiceMessageCallback.class);
        WebServiceMessageCallback requestCallback = (WebServiceMessageCallback) callbackControl.getMock();
        requestCallback.doInMessage(null);
        callbackControl.setMatcher(MockControl.ALWAYS_MATCHER);
        callbackControl.replay();

        MockControl extractorControl = MockControl.createControl(WebServiceMessageExtractor.class);
        WebServiceMessageExtractor extractorMock = (WebServiceMessageExtractor) extractorControl.getMock();
        extractorMock.extractData(null);
        extractorControl.setMatcher(MockControl.ALWAYS_MATCHER);
        Object extracted = new Object();
        extractorControl.setReturnValue(extracted);
        extractorControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), new MockTransportInputStream(
                new ByteArrayInputStream("<response/>".getBytes("UTF-8")), Collections.EMPTY_MAP));
        connectionControl.expectAndReturn(connectionMock.hasFault(), false);
        connectionMock.close();
        connectionControl.replay();

        Object result = template.sendAndReceive(requestCallback, extractorMock);
        assertEquals("Invalid response", extracted, result);

        callbackControl.verify();
        extractorControl.verify();
        connectionControl.verify();

    }

    public void testSendAndReceiveMessageNoResponse() throws Exception {
        MockControl callbackControl = MockControl.createControl(WebServiceMessageCallback.class);
        WebServiceMessageCallback requestCallback = (WebServiceMessageCallback) callbackControl.getMock();
        requestCallback.doInMessage(null);
        callbackControl.setMatcher(MockControl.ALWAYS_MATCHER);
        callbackControl.replay();

        MockControl extractorControl = MockControl.createControl(WebServiceMessageExtractor.class);
        WebServiceMessageExtractor extractorMock = (WebServiceMessageExtractor) extractorControl.getMock();
        extractorControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), null);
        connectionMock.close();
        connectionControl.replay();

        Object result = (WebServiceMessage) template.sendAndReceive(requestCallback, extractorMock);
        assertNull("Invalid response", result);
        callbackControl.verify();
        extractorControl.verify();
        connectionControl.verify();
    }

    public void testSendAndReceiveMessageFault() throws Exception {
        MockControl callbackControl = MockControl.createControl(WebServiceMessageCallback.class);
        WebServiceMessageCallback requestCallback = (WebServiceMessageCallback) callbackControl.getMock();
        requestCallback.doInMessage(null);
        callbackControl.setMatcher(MockControl.ALWAYS_MATCHER);
        callbackControl.replay();

        MockControl extractorControl = MockControl.createControl(WebServiceMessageExtractor.class);
        WebServiceMessageExtractor extractorMock = (WebServiceMessageExtractor) extractorControl.getMock();
        extractorControl.replay();

        MockControl faultResolverControl = MockControl.createControl(FaultResolver.class);
        FaultResolver faultResolverMock = (FaultResolver) faultResolverControl.getMock();
        template.setFaultResolver(faultResolverMock);
        faultResolverMock.resolveFault(null);
        faultResolverControl.setMatcher(MockControl.ALWAYS_MATCHER);
        faultResolverControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), new MockTransportInputStream(
                new ByteArrayInputStream("<response/>".getBytes("UTF-8")), Collections.EMPTY_MAP));
        connectionControl.expectAndReturn(connectionMock.hasFault(), true);
        connectionMock.close();
        connectionControl.replay();

        Object result = template.sendAndReceive(requestCallback, extractorMock);
        assertNull("Invalid response", result);

        callbackControl.verify();
        extractorControl.verify();
        connectionControl.verify();
    }

    public void testSendAndReceiveSourceResponse() throws Exception {
        MockControl extractorControl = MockControl.createControl(SourceExtractor.class);
        SourceExtractor extractorMock = (SourceExtractor) extractorControl.getMock();
        extractorMock.extractData(null);
        extractorControl.setMatcher(MockControl.ALWAYS_MATCHER);
        Object extracted = new Object();
        extractorControl.setReturnValue(extracted);
        extractorControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), new MockTransportInputStream(
                new ByteArrayInputStream("<response/>".getBytes("UTF-8")), Collections.EMPTY_MAP));
        connectionControl.expectAndReturn(connectionMock.hasFault(), false);
        connectionMock.close();
        connectionControl.replay();

        Object result = template.sendAndReceive(new StringSource("<request />"), extractorMock);
        assertEquals("Invalid response", extracted, result);

        extractorControl.verify();
        connectionControl.verify();
    }

    public void testSendAndReceiveSourceNoResponse() throws Exception {
        MockControl extractorControl = MockControl.createControl(SourceExtractor.class);
        SourceExtractor extractorMock = (SourceExtractor) extractorControl.getMock();
        extractorControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), null);
        connectionMock.close();
        connectionControl.replay();

        Object result = template.sendAndReceive(new StringSource("<request />"), extractorMock);
        assertNull("Invalid response", result);

        extractorControl.verify();
        connectionControl.verify();
    }

    public void testSendAndReceiveResultResponse() throws Exception {
        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), new MockTransportInputStream(
                new ByteArrayInputStream("<response/>".getBytes("UTF-8")), Collections.EMPTY_MAP));
        connectionControl.expectAndReturn(connectionMock.hasFault(), false);
        connectionMock.close();
        connectionControl.replay();

        StringResult result = new StringResult();
        template.sendAndReceive(new StringSource("<request />"), result);

        connectionControl.verify();
    }

    public void testSendAndReceiveMarshalResponse() throws Exception {
        MockControl marshallerControl = MockControl.createControl(Marshaller.class);
        Marshaller marshallerMock = (Marshaller) marshallerControl.getMock();
        template.setMarshaller(marshallerMock);
        marshallerMock.marshal(null, null);
        marshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        marshallerControl.replay();

        MockControl unmarshallerControl = MockControl.createControl(Unmarshaller.class);
        Unmarshaller unmarshallerMock = (Unmarshaller) unmarshallerControl.getMock();
        template.setUnmarshaller(unmarshallerMock);
        unmarshallerMock.unmarshal(null);
        unmarshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        Object unmarshalled = new Object();
        unmarshallerControl.setReturnValue(unmarshalled);
        unmarshallerControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), new MockTransportInputStream(
                new ByteArrayInputStream("<response/>".getBytes("UTF-8")), Collections.EMPTY_MAP));
        connectionControl.expectAndReturn(connectionMock.hasFault(), false);
        connectionMock.close();
        connectionControl.replay();

        Object result = template.marshalSendAndReceive(new Object());
        assertEquals("Invalid result", unmarshalled, result);

        connectionControl.verify();
        marshallerControl.verify();
        unmarshallerControl.verify();
    }

    public void testSendAndReceiveMarshalNoResponse() throws Exception {
        MockControl marshallerControl = MockControl.createControl(Marshaller.class);
        Marshaller marshallerMock = (Marshaller) marshallerControl.getMock();
        template.setMarshaller(marshallerMock);
        marshallerMock.marshal(null, null);
        marshallerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        marshallerControl.replay();

        MockControl unmarshallerControl = MockControl.createControl(Unmarshaller.class);
        Unmarshaller unmarshallerMock = (Unmarshaller) unmarshallerControl.getMock();
        template.setUnmarshaller(unmarshallerMock);
        unmarshallerControl.replay();

        connectionControl.expectAndReturn(connectionMock.getTransportOutputStream(),
                new MockTransportOutputStream(new ByteArrayOutputStream()));
        connectionControl.expectAndReturn(connectionMock.getTransportInputStream(), null);
        connectionMock.close();
        connectionControl.replay();

        Object result = template.marshalSendAndReceive(new Object());
        assertNull("Invalid result", result);

        connectionControl.verify();
        marshallerControl.verify();
        unmarshallerControl.verify();
    }

}