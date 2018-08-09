package com.jsparser.rhino;

class Pojo1 {
	public int a;
	public int b;
	
}

public class LamdaTest {
	public static void main(String[] args) {
		InnerClass li = new InnerClass();
		li.r2.run();
		System.out.println(" ************** USING LAMDA ************** ");
		InnerLamda il = new InnerLamda();
		il.r2.run();
		
		final int a = 5;
		final String st = "asdf";
		Pojo1 pobj1 = new Pojo1();
		final Pojo1 pobj = pobj1;

/*		a = 7;
		st = "34";
		pobj = new Pojo1();*/
		pobj.b = 78;
		
	}
}

class InnerClass {
	Runnable r2 = new Runnable() {
		@Override
		public void run() {
			//Runnable inner class toString
			System.out.println(this);
			System.out.println(this.toString());
			//Parent class toString
			System.out.println(InnerClass.this);
			System.out.println(InnerClass.this.toString());
		}
	};
	
	@Override
	public String toString() {
		return "This is InnerClass class toString." + super.toString();
	}
}

class InnerLamda {
	Runnable r2 = () -> {
		//InnerLamda class toString
		System.out.println(this);
		System.out.println(this.toString());
		//Parent class toString
		System.out.println(InnerLamda.this);
		System.out.println(InnerLamda.this.toString());
	};
	
	@Override
	public String toString() {
		return "This is InnerLamda class toString." + super.toString();
	}
}

