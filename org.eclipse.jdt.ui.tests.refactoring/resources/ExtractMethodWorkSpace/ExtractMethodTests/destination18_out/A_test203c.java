package destination_in;

public class A_test203 {
	@interface C {
		interface B {
			default void foo() {
				int i= /*[*/extracted();/*]*/				
			}
		}
	}

	protected static final synchronized int extracted() {
		return 0;
	}		
}