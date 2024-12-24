import javax.inject.Named;

public class TestMain {

	public static void main(String[] args) {
		System.out.println(findInterface(DogImpl.class, Dog.class));
		System.out.println(findInterface(DogImpl.class, Animal.class));
		System.out.println(findInterface(CatImpl.class, Dog.class));
	}

	private static boolean findInterface(Class<?> cls, Class<?> interf) {
		return interf.isAssignableFrom(cls);
	}

	public interface Animal {}
	public interface Dog extends Animal {}
	public interface Cat extends Animal {}

	@Named("dog")
	public static class DogImpl implements Dog {
	}

	@Named("cat")
	public static class CatImpl implements Cat {
	}
}
