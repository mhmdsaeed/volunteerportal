import 'package:flutter_test/flutter_test.dart';
import 'package:volunteer_portal_app/api/models.dart';
import 'package:volunteer_portal_app/screens/initiatives_screen.dart';

void main() {
  test('membership is read from the API, anything else meaning not joined', () {
    expect(Membership.parse('APPROVED'), Membership.approved);
    expect(Membership.parse('PENDING'), Membership.pending);
    expect(Membership.parse('REJECTED'), Membership.rejected);
    expect(Membership.parse('NONE'), Membership.none);
    expect(Membership.parse(null), Membership.none);
  });

  test('each filter shows the same memberships as the website', () {
    Set<Membership> shownBy(InitiativeFilter filter) => Membership.values.where(filter.matches).toSet();

    expect(shownBy(InitiativeFilter.joined), {Membership.approved});
    expect(shownBy(InitiativeFilter.pending), {Membership.pending});
    expect(shownBy(InitiativeFilter.rejected), {Membership.rejected});
    expect(shownBy(InitiativeFilter.notJoined), {Membership.none});
    expect(shownBy(InitiativeFilter.all), Membership.values.toSet());
  });

  InitiativeItem item(int id, {String? office, int? officeId}) => InitiativeItem(
      id: id, name: 'i$id', description: null, office: office, officeId: officeId, membership: Membership.none);

  test('the office choice works as on the website: all, one office by id, or none', () {
    final inYouth = item(1, office: 'Youth', officeId: 10);
    final withoutOffice = item(2);

    expect(inOffice(inYouth, null), isTrue);
    expect(inOffice(withoutOffice, null), isTrue);
    expect(inOffice(inYouth, '10'), isTrue);
    expect(inOffice(inYouth, '11'), isFalse);
    expect(inOffice(withoutOffice, '10'), isFalse);
    expect(inOffice(withoutOffice, noOffice), isTrue);
    expect(inOffice(inYouth, noOffice), isFalse);
  });

  test('the offices to choose from are listed once each, by name, by id even when names repeat', () {
    final offices = officesOf([
      item(1, office: 'youth', officeId: 10),
      item(2, office: 'Coast', officeId: 11),
      item(3, office: 'youth', officeId: 10),
      item(4),
      item(5, office: 'Youth', officeId: 12), // another office with the same name
    ]);

    expect(offices.map((o) => o.$1), [11, 10, 12]);
    expect(offices.map((o) => o.$2), ['Coast', 'youth', 'Youth']);
  });
}
