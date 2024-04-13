module JTableFX
{
  requires transitive javafx.base;
  requires transitive javafx.graphics;

  requires javafx.controls;

  exports rjc.table.demo;
  exports rjc.table.undo;
  exports rjc.table.signal;
}