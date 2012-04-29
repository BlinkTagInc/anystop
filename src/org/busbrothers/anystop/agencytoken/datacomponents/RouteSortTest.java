package org.busbrothers.anystop.agencytoken.datacomponents;

/** RouteSortTest implements a set of test cases for the Route class's compareTo functionality, and relevant methods. 
 * To get this to run comment out the package... line at the top of Route, and run Route/RouteSortTest from the command line using plain old javac.*/ 

public class RouteSortTest {
	
	public static void assertEquals(Object one, Object two) {
		if(!one.equals(two)) assert false;
	}
	
	public static void assertEquals(int one, int two) {
		if(one != two) assert false;
	}
	
	public static void assertPositive(int one) {
		assert (one > 0);
	}
	
	public static void assertNegative(int one) {
		assert (one < 0);
	}
	
	public static void assertZero(int one) {
		assert (one == 0);
	}

	public static void testCompareToForRouteName() {
		//Test case 1 should resolve with the 2nd string being greater
		assertNegative(Route.compareToForRouteName("2B", "10A"));
		
		//Test case 2 should resolve with them being equals
		assertZero(Route.compareToForRouteName("", ""));
		
		//Test case 3 should resolve with first being greater
		assertPositive(Route.compareToForRouteName("", "ABC213"));
		
		//Test case 4 should resolve with 2nd being greater
		assertNegative(Route.compareToForRouteName("ABC213", ""));
		
		//Test case 5 should resolve with 2nd being greater
		assertNegative(Route.compareToForRouteName("123ABD", "ABC"));
		
		//Test case 6 should resolve with them being equal
		assertZero(Route.compareToForRouteName("ABC", "ABC"));
		
		//Test case 7 should resolve with 1st being greater
		assertPositive(Route.compareToForRouteName("ABD", "ABC"));
		
		//Test case 8 should resolve with 2nd being greater
		assertNegative(Route.compareToForRouteName("123ABC", "123ABD"));
		
		//Test case 9 should resolve with the 2nd string being greater
		assertNegative(Route.compareToForRouteName("22ZZZZZ", "111A"));
		
		//Test case 10 should resolve with the 1st string being greater
		assertPositive(Route.compareToForRouteName("11Z", "11X"));
		
		System.err.println("Passed testCompareToForRouteName test cases.");
	}

	public static void testStripNonAlphaNum() {
		String testStringOne = "-=&*^123abc";
		String testStringTwo = "-=&*^abc123";
		String testStringThree = "-=&*^123%^&";
		String testStringFour = "-*&%";
		String testStringFive = "123abc";
		String testStringSix = "abc123";
		String testStringSeven = "ABC123";
		String testStringEight = "123ABC";
		
		String retval;
		retval = Route.stripNonAlphaNum(testStringOne);
		assertEquals(retval, "123abc");
		
		retval = Route.stripNonAlphaNum(testStringTwo);
		assertEquals(retval, "abc123");
		
		retval = Route.stripNonAlphaNum(testStringThree);
		assertEquals(retval, "123%^&");
		
		retval = Route.stripNonAlphaNum(testStringFour);
		assertEquals(retval, "");
		
		retval = Route.stripNonAlphaNum(testStringFive);
		assertEquals(retval, testStringFive);
		
		retval = Route.stripNonAlphaNum(testStringSix);
		assertEquals(retval, testStringSix);
		
		retval = Route.stripNonAlphaNum(testStringSeven);
		assertEquals(retval, testStringSeven);
		
		retval = Route.stripNonAlphaNum(testStringEight);
		assertEquals(retval, testStringEight);
		
		System.err.println("Passed testStripNonAlphaNum test cases.");
	}

	public static void testGetLeadingInteger() {
		String testStringOne = "123ABC";
		String testStringTwo = "ABC123";
		String testStringThree = "123";
		String testStringFour = "ABC";
		
		int retval;
		retval = Route.getLeadingInteger(testStringOne);
		assertEquals(retval, 123);
		
		retval = Route.getLeadingInteger(testStringTwo);
		assertEquals(retval, -1);
		
		retval = Route.getLeadingInteger(testStringThree);
		assertEquals(retval, 123);
		
		retval = Route.getLeadingInteger(testStringFour);
		assertEquals(retval, -1);
		
		System.err.println("Passed testGetLeadingInteger test cases.");
	}

	public static void testStripLeadingInteger() {
		String testStringOne = "123ABC";
		String testStringTwo = "ABC123";
		String testStringThree = "123";
		String testStringFour = "ABC";
		
		String retval;
		retval = Route.stripLeadingInteger(testStringOne);
		assertEquals(retval, "ABC");
		
		retval = Route.stripLeadingInteger(testStringTwo);
		assertEquals(retval, "ABC123");
		
		retval = Route.stripLeadingInteger(testStringThree);
		assertEquals(retval, "");
		
		retval = Route.stripLeadingInteger(testStringFour);
		assertEquals(retval, "ABC");
		
		System.err.println("Passed testStripLeadingInteger test cases.");
	}
	
	public static void main (String [] args) {
		System.err.println("Beginning testStripNonAlphaNum test...");
		testStripNonAlphaNum();
		System.err.println("Beginning testGetLeadingInteger test...");
		testGetLeadingInteger();
		System.err.println("Beginning testStripLeadingInteger test...");
		testStripLeadingInteger();
		System.err.println("Beginning testCompareToForRouteName test...");
		testCompareToForRouteName();
		System.err.println("MAIN: All tests finished running.");
	}

}
