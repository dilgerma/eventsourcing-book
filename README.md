## Nebulit GmbH - Eventmodeling Template

### Setup

Slices sind im _root_ Package (wie im Generator angegeben) als Packages definiert.

### Todos nach der initialen Generierung

Im Code sind TODOs definiert für die Stellen die angepasst werden müssen.
Der Generator trifft bestimmte Grundannahmen (aggregateIds sind UUIDs beispielsweise).

Wird von diesen Annahmen abgewichen kompiliert der Code ggf. nicht sofort sondern muss leicht
angepasst werden.

Ihre Code Richtlinien sind natürlich führend, daher ist es erwartungskonform dass Code
nicht sofort kompiliert (es sollten aber wirklich nur kleine Anpassungen notwendig sein).

### Start der Applikation

Zum Start des Services kann die Klasse _ApplicationStarter_ verwendet werden in _src/test/kotlin_.
Warum in _test_?

Diese Klasse startet die komplette Umgebung (inkl. Postgres und ggf. Kafka über TestContainers)

### Package Struktur

Events sind im Package "events"

Aggregates liegen im Package "domain"

Slices haben jeweils ein isoliertes Package <sliceName>

Package "common" enthält einige Interfaces für die generelle Struktur.
