package github.vanes430.orderplugin.model;

public enum SortType {
   MOST_PAID("Most Paid"),
   MOST_DELIVERED("Most Delivered"),
   RECENTLY_LISTED("Recently Listed"),
   MOST_MONEY_PER_ITEM("Most Money Per Item");

   private final String displayName;

   private SortType(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public SortType next() {
      SortType[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }
}
