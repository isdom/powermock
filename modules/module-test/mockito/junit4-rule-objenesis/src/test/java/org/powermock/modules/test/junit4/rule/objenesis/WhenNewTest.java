/*
 * Copyright 2008 the original author or authors.
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
package org.powermock.modules.test.junit4.rule.objenesis;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.powermock.reflect.exceptions.ConstructorNotFoundException;
import samples.Service;
import samples.expectnew.ExpectNewDemo;
import samples.expectnew.ExpectNewServiceUser;
import samples.expectnew.VarArgsConstructorDemo;
import samples.newmocking.MyClass;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.constructor;

/**
 * Test class to demonstrate new instance mocking using whenConstructionOf(..).
 * 
 */
@Ignore("Since upgrading to JVM 1.6.0_24 lots of tests started to fail")
@PrepareForTest({ MyClass.class, ExpectNewDemo.class, DataInputStream.class })
public class WhenNewTest {
	@Rule
	public PowerMockRule powerMockRule = new PowerMockRule();

	@Test
	public void testNewWithCheckedException() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		final String expectedFailMessage = "testing checked exception";
		whenNew(MyClass.class).withNoArguments().thenThrow(new IOException(expectedFailMessage));

		try {
			tested.throwExceptionAndWrapInRunTimeWhenInvoction();
			fail("Should throw a checked Exception!");
		} catch (RuntimeException e) {
			assertTrue(e.getCause() instanceof IOException);
			assertEquals(expectedFailMessage, e.getMessage());
		}

		verifyNew(MyClass.class).withNoArguments();
	}

	@Test
	public void testGetMessage() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		MyClass myClassMock = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock);

		String expected = "Hello altered World";
		when(myClassMock.getMessage()).thenReturn("Hello altered World");

		String actual = tested.getMessage();

		verify(myClassMock).getMessage();
		verifyNew(MyClass.class).withNoArguments();
		assertEquals("Expected and actual did not match", expected, actual);
	}

	@Test
	public void testGetMessageWithArgument() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock = mock(MyClass.class);
		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock);

		String expected = "Hello altered World";
		when(myClassMock.getMessage("test")).thenReturn("Hello altered World");

		String actual = tested.getMessageWithArgument();

		verify(myClassMock).getMessage("test");
		verifyNew(MyClass.class).withNoArguments();
		assertEquals("Expected and actual did not match", expected, actual);
	}

	@Test
	public void testInvokeVoidMethod() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock = mock(MyClass.class);
		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock);

		doNothing().when(myClassMock).voidMethod();

		tested.invokeVoidMethod();

		verify(myClassMock).voidMethod();
		verifyNew(MyClass.class).withNoArguments();
	}

	@Test
	public void testNewWithRuntimeException() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		final String expectedFailMessage = "testing";
		whenNew(MyClass.class).withNoArguments().thenThrow(new RuntimeException(expectedFailMessage));

		try {
			tested.throwExceptionWhenInvoction();
			fail("Should throw RuntimeException!");
		} catch (RuntimeException e) {
			assertEquals(expectedFailMessage, e.getMessage());
		}

		verifyNew(MyClass.class).withNoArguments();
	}

	@Test
	public void testMultipleNew() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock);

		when(myClassMock.getMessage()).thenReturn("Hello");

		final String actual = tested.multipleNew();

		verify(myClassMock, times(2)).getMessage();
		verifyNew(MyClass.class, times(2)).withNoArguments();

		assertEquals("HelloHello", actual);
	}

	@Test
	public void testSimpleMultipleNew() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock);

		tested.simpleMultipleNew();

		verifyNew(MyClass.class, times(3)).withNoArguments();
	}

	@Test
	public void testSimpleMultipleNew_tooManyTimesExpected() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock);

		tested.simpleMultipleNew();

		try {
			verifyNew(MyClass.class, times(4)).withNoArguments();
			fail("Should throw AssertionError.");
		} catch (AssertionError e) {
			assertEquals("samples.newmocking.MyClass();\nWanted 4 times but was 3 times.", e.getMessage());
		}
	}

	@Test
	public void testSimpleMultipleNew_tooFewTimesExpected() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		tested.simpleMultipleNew();
		try {
			verifyNew(MyClass.class, times(1)).withNoArguments();
			fail("Should throw AssertionError.");
		} catch (AssertionError e) {
			assertEquals("samples.newmocking.MyClass();\nWanted 1 time but was 3 times.", e.getMessage());
		}
	}

	/**
	 * Verifies that the issue
	 * http://code.google.com/p/powermock/issues/detail?id=10 is solved.
	 */
	@Test
	public void testSimpleMultipleNewPrivate_tooFewTimesExpected() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		Whitebox.invokeMethod(tested, "simpleMultipleNewPrivate");

		try {
			verifyNew(MyClass.class, times(2)).withNoArguments();
			fail("Should throw AssertionError.");
		} catch (AssertionError e) {
			assertEquals("samples.newmocking.MyClass();\nWanted 2 times but was 3 times.", e.getMessage());
		}
	}

	@Test
	public void testSimpleMultipleNewPrivate_ok() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		Whitebox.invokeMethod(tested, "simpleMultipleNewPrivate");

		verifyNew(MyClass.class, times(3)).withNoArguments();
	}

	@Test
	public void testSimpleSingleNew_withOnce() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		tested.simpleSingleNew();

		verifyNew(MyClass.class).withNoArguments();
	}

	@Test
	public void testSimpleSingleNew_withAtLeastOnce() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		tested.simpleSingleNew();

		verifyNew(MyClass.class, atLeastOnce()).withNoArguments();
	}

	@Test
	public void testSimpleMultipleNew_withAtLeastOnce() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		tested.simpleMultipleNew();

		verifyNew(MyClass.class, atLeastOnce()).withNoArguments();
	}

	//
	@Test
	public void testAlternativeFlow() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		whenNew(DataInputStream.class).withArguments(null).thenThrow(new RuntimeException("error"));

		InputStream stream = tested.alternativePath();

		verifyNew(DataInputStream.class).withArguments(null);

		assertNotNull("The returned inputstream should not be null.", stream);
		assertTrue("The returned inputstream should be an instance of ByteArrayInputStream.",
				stream instanceof ByteArrayInputStream);
	}

	@Test
	public void testSimpleMultipleNewPrivate_tooManyTimesExpected() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		Whitebox.invokeMethod(tested, "simpleMultipleNewPrivate");
		try {
			verifyNew(MyClass.class, times(4)).withNoArguments();
			fail("Should throw an exception!.");
		} catch (AssertionError e) {
			assertEquals("samples.newmocking.MyClass();\nWanted 4 times but was 3 times.", e.getMessage());
		}
	}

	@Test
	public void testNewWithArguments() throws Exception {
		final int numberOfTimes = 2;
		final String expected = "used";

		ExpectNewDemo tested = new ExpectNewDemo();
		ExpectNewServiceUser expectNewServiceImplMock = mock(ExpectNewServiceUser.class);
		Service serviceMock = mock(Service.class);

		whenNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes).thenReturn(
				expectNewServiceImplMock);
		when(expectNewServiceImplMock.useService()).thenReturn(expected);

		assertEquals(expected, tested.newWithArguments(serviceMock, numberOfTimes));

		verifyNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes);
	}

	@Test
	public void testNewWithParameterTypesAndArguments() throws Exception {
		final int numberOfTimes = 2;
		final String expected = "used";

		ExpectNewDemo tested = new ExpectNewDemo();
		ExpectNewServiceUser expectNewServiceImplMock = mock(ExpectNewServiceUser.class);
		Service serviceMock = mock(Service.class);

		whenNew(ExpectNewServiceUser.class).withParameterTypes(Service.class, int.class)
				.withArguments(serviceMock, numberOfTimes).thenReturn(expectNewServiceImplMock);
		when(expectNewServiceImplMock.useService()).thenReturn(expected);

		assertEquals(expected, tested.newWithArguments(serviceMock, numberOfTimes));

		verifyNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes);
	}

	@Test
	public void testNewWithConstructorUsingParameterTypesAndArguments() throws Exception {
		final int numberOfTimes = 2;
		final String expected = "used";

		ExpectNewDemo tested = new ExpectNewDemo();
		ExpectNewServiceUser expectNewServiceImplMock = mock(ExpectNewServiceUser.class);
		Service serviceMock = mock(Service.class);

		whenNew(constructor(ExpectNewServiceUser.class, Service.class, int.class)).withArguments(serviceMock,
				numberOfTimes).thenReturn(expectNewServiceImplMock);
		when(expectNewServiceImplMock.useService()).thenReturn(expected);

		assertEquals(expected, tested.newWithArguments(serviceMock, numberOfTimes));

		verifyNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes);
	}

	@Test
	public void testNewUsingConstructorWithArguments() throws Exception {
		final int numberOfTimes = 2;
		final String expected = "used";

		ExpectNewDemo tested = new ExpectNewDemo();
		ExpectNewServiceUser expectNewServiceImplMock = mock(ExpectNewServiceUser.class);
		Service serviceMock = mock(Service.class);

		whenNew(constructor(ExpectNewServiceUser.class)).withArguments(serviceMock, numberOfTimes).thenReturn(
				expectNewServiceImplMock);
		when(expectNewServiceImplMock.useService()).thenReturn(expected);

		assertEquals(expected, tested.newWithArguments(serviceMock, numberOfTimes));

		verifyNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes);
	}

	@Test
	public void testNewWithVarArgs() throws Exception {
		final String firstString = "hello";
		final String secondString = "world";

		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		whenNew(VarArgsConstructorDemo.class).withArguments(firstString, secondString).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getAllMessages()).thenReturn(new String[] { firstString, secondString });

		String[] varArgs = tested.newVarArgs(firstString, secondString);
		assertEquals(2, varArgs.length);
		assertEquals(firstString, varArgs[0]);
		assertEquals(secondString, varArgs[1]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(firstString, secondString);
	}

	@Test
	public void testNewWhenTheExpectedConstructorIsNotFound() throws Exception {
		final Object object = new Object();
		try {
			whenNew(VarArgsConstructorDemo.class).withArguments(object);
			fail("Should throw ConstructorNotFoundException!");
		} catch (ConstructorNotFoundException e) {
			assertEquals("No constructor found in class '" + VarArgsConstructorDemo.class.getName()
					+ "' with parameter types: [ " + object.getClass().getName() + " ].", e.getMessage());
		}
	}

	@Test
	public void testNewWithVarArgsConstructorWhenOneArgumentIsOfASubType() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		Service serviceMock = mock(Service.class);
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final Service serviceSubTypeInstance = new Service() {

			public String getServiceMessage() {
				return "message";
			}
		};

		whenNew(VarArgsConstructorDemo.class).withArguments(serviceSubTypeInstance, serviceMock).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getAllServices()).thenReturn(new Service[] { serviceMock });

		Service[] varArgs = tested.newVarArgs(serviceSubTypeInstance, serviceMock);
		assertEquals(1, varArgs.length);
		assertSame(serviceMock, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(serviceSubTypeInstance, serviceMock);
	}

	@Test
	public void testNewWithArrayVarArgs() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final byte[] byteArrayOne = new byte[] { 42 };
		final byte[] byteArrayTwo = new byte[] { 17 };
		whenNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getByteArrays()).thenReturn(new byte[][] { byteArrayOne });

		byte[][] varArgs = tested.newVarArgs(byteArrayOne, byteArrayTwo);
		assertEquals(1, varArgs.length);
		assertSame(byteArrayOne, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo);
	}

	@Test
	public void testNewWithArrayVarArgsAndMatchers() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final byte[] byteArrayOne = new byte[] { 42 };
		final byte[] byteArrayTwo = new byte[] { 17 };
		whenNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getByteArrays()).thenReturn(new byte[][] { byteArrayOne });

		byte[][] varArgs = tested.newVarArgsWithMatchers();
		assertEquals(1, varArgs.length);
		assertSame(byteArrayOne, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo);
	}

	@Test
	public void testNewWithArrayVarArgsWhenFirstArgumentIsNullAndSubseqentArgumentsAreNotNull() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final byte[] byteArrayOne = null;
		final byte[] byteArrayTwo = new byte[] { 17 };
		whenNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getByteArrays()).thenReturn(new byte[][] { byteArrayTwo });

		byte[][] varArgs = tested.newVarArgs(byteArrayOne, byteArrayTwo);
		assertEquals(1, varArgs.length);
		assertSame(byteArrayTwo, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo);
	}

	@Test
	public void testNewWithArrayVarArgsWhenFirstArgumentIsNotNullButSubseqentArgumentsAreNull() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final byte[] byteArrayOne = new byte[] { 42 };
		final byte[] byteArrayTwo = null;
		whenNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getByteArrays()).thenReturn(new byte[][] { byteArrayOne });

		byte[][] varArgs = tested.newVarArgs(byteArrayOne, byteArrayTwo);
		assertEquals(1, varArgs.length);
		assertSame(byteArrayOne, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo);
	}

	@Test
	public void testNewWithArrayVarArgsWhenFirstArgumentIsNullSecondArgumentIsNotNullAndThirdArgumentIsNull()
			throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final byte[] byteArrayOne = null;
		final byte[] byteArrayTwo = new byte[] { 42 };
		final byte[] byteArrayThree = null;
		whenNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo, byteArrayThree).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getByteArrays()).thenReturn(new byte[][] { byteArrayTwo });

		byte[][] varArgs = tested.newVarArgs(byteArrayOne, byteArrayTwo, byteArrayThree);
		assertEquals(1, varArgs.length);
		assertSame(byteArrayTwo, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo, byteArrayThree);
	}

	@Test
	public void testNewWithArrayVarArgsWhenAllArgumentsAreNull() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();
		VarArgsConstructorDemo varArgsConstructorDemoMock = mock(VarArgsConstructorDemo.class);

		final byte[] byteArrayOne = null;
		final byte[] byteArrayTwo = null;
		whenNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo).thenReturn(
				varArgsConstructorDemoMock);
		when(varArgsConstructorDemoMock.getByteArrays()).thenReturn(new byte[][] { byteArrayTwo });

		byte[][] varArgs = tested.newVarArgs(byteArrayOne, byteArrayTwo);
		assertEquals(1, varArgs.length);
		assertSame(byteArrayTwo, varArgs[0]);

		verifyNew(VarArgsConstructorDemo.class).withArguments(byteArrayOne, byteArrayTwo);
	}

	@Test(expected = NullPointerException.class)
	public void testNewWithWrongArgument() throws Exception {
		final int numberOfTimes = 2;
		final String expected = "used";

		ExpectNewDemo tested = new ExpectNewDemo();
		ExpectNewServiceUser expectNewServiceImplMock = mock(ExpectNewServiceUser.class);
		Service serviceMock = mock(Service.class);

		whenNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes).thenReturn(
				expectNewServiceImplMock);
		when(expectNewServiceImplMock.useService()).thenReturn(expected);

		assertEquals(expected, tested.newWithWrongArguments(serviceMock, numberOfTimes));

		verifyNew(ExpectNewServiceUser.class).withArguments(serviceMock, numberOfTimes);
		/*
		 * Should throw NPE because the default behavior of Mockito when a
		 * something isn't expected is to return a default value. In this case
		 * whenConstructionOf
		 * (ExpectNewServiceUser.class).withArguments(serviceMock,
		 * numberOfTimes) is the wrong expectation and thus null is returned
		 * from the substitute mock which is the correct behavior.
		 */
		fail("Should throw NPE!");
	}

	@Test
	public void testExpectNewButNoNewCallWasMade() throws Exception {
		ExpectNewDemo tested = new ExpectNewDemo();

		MyClass myClassMock1 = mock(MyClass.class);

		whenNew(MyClass.class).withNoArguments().thenReturn(myClassMock1);

		tested.makeDate();

		try {
			verifyNew(MyClass.class).withNoArguments();
			fail("Should throw AssertionError!");
		} catch (AssertionError e) {
			assertEquals(
					"Wanted but not invoked samples.newmocking.MyClass();\nActually, there were zero interactions with this mock.",
					e.getMessage());
		}
	}
}
