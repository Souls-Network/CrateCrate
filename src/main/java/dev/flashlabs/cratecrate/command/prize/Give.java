package dev.flashlabs.cratecrate.command.prize;

import dev.flashlabs.cratecrate.CrateCrate;
import dev.flashlabs.cratecrate.command.CommandUtils;
import dev.flashlabs.cratecrate.component.prize.CommandPrize;
import dev.flashlabs.cratecrate.component.prize.ItemPrize;
import dev.flashlabs.cratecrate.component.prize.MoneyPrize;
import dev.flashlabs.cratecrate.component.prize.Prize;
import dev.flashlabs.cratecrate.internal.Config;
import dev.flashlabs.cratecrate.internal.Utils;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.util.locale.LocaleSource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public final class Give {
    public static final Component USAGE = CommandUtils.usage(
            "/crate prize give ",
            "Gives a prize to a user.",
            CommandUtils.argument("user", false, "A username or selector matching a single user (online/offline), defaulting to the player executing this command."),
            CommandUtils.argument("prize", true, "A registered prize id."),
            CommandUtils.argument("value", false, "A reference value for the prize (varies by type).\n\n - Command: The ${value} placeholder\n - Item: The integer quantity\n - Money: The decimal amount")
    );

    public static Command.Parameterized COMMAND = Command.builder()
        .permission("cratecrate.command.prize.give.base")
        .addParameter(Parameter.user().key("user").build())
        .addParameter(Parameter.choices(Prize.class, Config.PRIZES::get, Config.PRIZES::keySet).key("prize").build())
        .addParameter(Parameter.remainingJoinedStrings().optional().key("value").build())
        .executor(Give::execute)
        .build();

    private static CommandResult execute(CommandContext context) throws CommandException {
        var user = Utils.user(context, "user");
        var prize = context.requireOne(Parameter.key("prize", TypeToken.get(Prize.class)));
        var value = context.one(Parameter.key("value", String.class));

        boolean result;

        if (prize instanceof CommandPrize) {
            //TODO: Check for unused value
            result = prize.give(user, value.orElse(""));
        } else if (prize instanceof ItemPrize) {
            try {
                int quantity = value.map(Integer::parseInt).orElse(1);
                //TODO: Check max quantity
                if (quantity <= 0) {
                    throw new NumberFormatException();
                }
                result = prize.give(user, quantity);
            } catch (NumberFormatException e) {
                throw new CommandException(CrateCrate.get().getMessage("command.prize.give.invalid-quantity", ((LocaleSource) context.cause().audience()).locale(),
                        "quantity", value.map(v -> v.contains(" ") ? "\"" + v + "\"" : v).orElse("\"\""),
                        "bound", 0
                ));
            }
        } else if (prize instanceof MoneyPrize) {
            try {
                BigDecimal amount = value.map(BigDecimal::new).orElse(BigDecimal.ZERO);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException();
                }
                result = prize.give(user, amount);
            } catch (NumberFormatException e) {
                throw new CommandException(CrateCrate.get().getMessage("command.prize.give.invalid-amount", ((LocaleSource) context.cause().audience()).locale(),
                        "amount", value.map(v -> v.contains(" ") ? "\"" + v + "\"" : v).orElse("\"\""),
                        "bound", 0
                ));
            }
        } else {
            throw new AssertionError(prize.getClass().getName());
        }
        if (result) {
            CrateCrate.get().sendMessage((Audience & LocaleSource) context.cause().audience(), "command.prize.give.success",
                    "user", user.name(),
                    "prize", prize.id(),
                    "value", value.map(v -> v.contains(" ") ? "\"" + v + "\"" : v).orElse("\"\"")
            );
        } else {
            throw new CommandException(CrateCrate.get().getMessage("command.prize.give.failure", ((LocaleSource) context.cause().audience()).locale(),
                    "user", user.name(),
                    "prize", prize.id(),
                    "value", value.map(v -> v.contains(" ") ? "\"" + v + "\"" : v).orElse("\"\"")
            ));
        }
        return CommandResult.success();
    }

}
