package com.github.chen0040.lda;


/**
 * Created by xschen on 4/5/2017.
 */
public class TupleTwo<T, T2> {
   private final T v1;
   private final T2 v2;

   public TupleTwo(T v1, T2 v2) {
      this.v1 = v1;
      this.v2 = v2;
   }

   public T _1(){
      return v1;
   }

   public T2 _2(){
      return v2;
   }


   @Override public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      TupleTwo<?, ?> tupleTwo = (TupleTwo<?, ?>) o;

      if (v1 != null ? !v1.equals(tupleTwo.v1) : tupleTwo.v1 != null)
         return false;
      return v2 != null ? v2.equals(tupleTwo.v2) : tupleTwo.v2 == null;

   }


   @Override public int hashCode() {
      int result = v1 != null ? v1.hashCode() : 0;
      result = 31 * result + (v2 != null ? v2.hashCode() : 0);
      return result;
   }
}
