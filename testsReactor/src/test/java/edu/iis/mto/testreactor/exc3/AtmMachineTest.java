package edu.iis.mto.testreactor.exc3;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.AuthenticationException;
import java.util.ArrayList;
import java.util.List;

public class AtmMachineTest {

    private AtmMachine atmMachine;

    private CardProviderService cardProviderService;

    private BankService bankService;

    private MoneyDepot moneyDepot;

    @Before
    public void setup() {
        cardProviderService = mock(CardProviderService.class);
        bankService = mock(BankService.class);
        moneyDepot = mock(MoneyDepot.class);
        atmMachine = new AtmMachine(cardProviderService, bankService, moneyDepot);
    }

    @Test
    public void itCompiles() {
        assertThat(true, equalTo(true));
    }

    @Test
    (expected = AtmException.class)
    public void withdrawThrowsAtmExceptionIfAuthorizationFails() {
        Money money = Money.builder()
                           .withAmount(100)
                           .withCurrency(Currency.PL)
                           .build();

        Card card = Card.builder()
                        .withCardNumber("123")
                        .withPinNumber(123)
                        .build();

        try {
            Mockito.when(cardProviderService.authorize(card)).thenThrow(CardAuthorizationException.class);
        } catch (CardAuthorizationException e) {
            e.printStackTrace();
        }

        atmMachine.withdraw(money, card);
    }

    @Test
    public void withdrawReturnsExpectedBanknotePayment() {
        Money money = Money.builder()
                           .withAmount(100)
                           .withCurrency(Currency.PL)
                           .build();

        Card card = Card.builder()
                        .withCardNumber("123")
                        .withPinNumber(123)
                        .build();

        Payment result = atmMachine.withdraw(money, card);
        List<Banknote> banknotes = new ArrayList<>();
        banknotes.add(Banknote.PL100);
        assertThat(result, is(new Payment(banknotes)));
    }

    @Test
    (expected = WrongMoneyAmountException.class)
    public void withdrawThrowsWrongMoneyAmountExceptionIfMoneyAmountCannotBePaidWithBanknotes() {
        Money money = Money.builder()
                           .withAmount(1)
                           .withCurrency(Currency.PL)
                           .build();

        Card card = Card.builder()
                        .withCardNumber("123")
                        .withPinNumber(123)
                        .build();

        atmMachine.withdraw(money, card);
    }

    @Test
    public void withdrawShouldCallAuthorizeOnce() throws CardAuthorizationException {
        Money money = Money.builder()
                           .withAmount(100)
                           .withCurrency(Currency.PL)
                           .build();

        Card card = Card.builder()
                        .withCardNumber("123")
                        .withPinNumber(123)
                        .build();

        AuthenticationToken authenticationToken = AuthenticationToken.builder().withAuthorizationCode(123).withUserId("1").build();
        Mockito.when(cardProviderService.authorize(card)).thenReturn(authenticationToken);

        atmMachine.withdraw(money, card);
        verify(cardProviderService, atLeastOnce()).authorize(card);
    }

    @Test
    (expected = AtmException.class)
    public void withdrawWithInsufficientFundsThrowsAtmException() throws InsufficientFundsException, CardAuthorizationException {
        Money money = Money.builder()
                           .withAmount(100)
                           .withCurrency(Currency.PL)
                           .build();

        Card card = Card.builder()
                        .withCardNumber("123")
                        .withPinNumber(123)
                        .build();

        AuthenticationToken authenticationToken = AuthenticationToken.builder().withAuthorizationCode(123).withUserId("1").build();
        Mockito.when(cardProviderService.authorize(card)).thenReturn(authenticationToken);
        doThrow(InsufficientFundsException.class).when(bankService).charge(authenticationToken, money);

        atmMachine.withdraw(money, card);
    }
}
