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
}
