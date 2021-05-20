package com.game.service;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.transaction.Transactional;
import java.util.*;

@Service
public class PlayerService {
    private List<Player> playerList = new ArrayList<>();
    @Autowired
    private PlayerRepository repository;

    @Transactional
    public List<Player> getAllPlayers(){
        playerList.addAll((List<Player>)repository.findAll());
        return playerList;
    }

    @Transactional
    public List<Player> getFilteredPlayers(
            String name, String title,
            Race race, Profession profession,
            Long after, Long before,
            String banned,
            Integer minExperience, Integer maxExperience,
            Integer minLevel, Integer maxLevel){
        List<Player> resultList = new ArrayList<>();
        final Date afterDate = after == null ? null : new Date(after);
        final Date beforeDate = before == null ? null : new Date(before);
        repository.findAll().forEach(player -> {
            if (name!=null && !player.getName().contains(name)) return;
            if (title!=null && !player.getTitle().contains(title)) return;
            if (race != null && player.getRace() != race) return;
            if (profession != null && player.getProfession() != profession) return;
            if (after != null && player.getBirthday().before(afterDate)) return;
            if (before != null && player.getBirthday().after(beforeDate)) return;
            if (banned != null && player.isBanned() != Boolean.valueOf(banned)) return;
            if (minExperience != null && player.getExperience().compareTo(minExperience) < 0) return;
            if (maxExperience != null && player.getExperience().compareTo(maxExperience) > 0) return;
            if (minLevel != null && player.getLevel().compareTo(minLevel) < 0) return;
            if (maxLevel != null && player.getLevel().compareTo(maxLevel) > 0) return;
            resultList.add(player);
        });
        return resultList;
    }

    @Transactional
    public Player save(Player player){
        if (player.getExperience() == null
                || player.getTitle() == null || player.getName() == null
                || player.getTitle().length() > 30 || player.getName().length() > 30
                || player.getRace() == null || player.getProfession() == null
                || player.getBirthday() == null
                || player.getBirthday().before(new Date(946684800000L))
                || player.getBirthday().after(new Date(32503680000000L))
                || player.getExperience() < 0
                || player.getExperience() > 10000000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        player.setLevel((int) (Math.sqrt((double) 2500 + 200 * player.getExperience()) - 50) / 100);
        player.setUntilNextLevel(50 * (player.getLevel() + 1) * (player.getLevel() + 2) - player.getExperience());
        return repository.save(player);
    }

    @Transactional
    public Player update(Player player, Long id){
        Player resultPlayer;
        if (id == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        else if (id != null && repository.findById(id).equals(Optional.empty()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            player.getExperience();
            if (player.getExperience() < 0 || player.getExperience() > 10000000)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (NullPointerException e){
        }
        if (id != null
                && player.getTitle() == null && player.getName() == null
                && player.getBirthday() == null
                && player.getExperience() == null
                && player.getProfession() == null
                && player.getUntilNextLevel() == null
                && player.getLevel() == null
                && player.isBanned() == false
                && player.getUntilNextLevel() == null)
            return repository.findById(id).get();
        else {
            if (player.getBirthday() != null && player.getBirthday().getTime() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            if (player.getBirthday() != null && !player.getBirthday().after(new Date(946684800000L)) && !player.getBirthday().before(new Date(32503680000000L)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            resultPlayer = repository.findById(id).get();
            if (player.getName() != null && player.getName().length() <= 12)
                resultPlayer.setName(player.getName());
            if (player.getTitle() != null && player.getTitle().length() <= 30)
                resultPlayer.setTitle(player.getTitle());
            if (player.getRace() != null)
                resultPlayer.setRace(player.getRace());
            if (player.getProfession() != null)
                resultPlayer.setProfession(player.getProfession());
            if (player.getBirthday() != null && player.getBirthday().after(new Date(946684800000L)) && player.getBirthday().before(new Date(32503680000000L)))
                resultPlayer.setBirthday(player.getBirthday());
            if (player.getExperience() != null && player.getExperience() >= 0 && player.getExperience() <= 10000000) {
                resultPlayer.setExperience(player.getExperience());
                resultPlayer.setLevel((int) (Math.sqrt((double) 2500 + 200 * player.getExperience()) - 50) / 100);
                resultPlayer.setUntilNextLevel(50 * (resultPlayer.getLevel() + 1) * (resultPlayer.getLevel() + 2) - player.getExperience());
                if (player.isBanned() != null)
                    resultPlayer.setBanned(player.isBanned());
            }
            repository.save(resultPlayer);
            return resultPlayer;
        }
    }

    @Transactional
    public Player getPlayerById(Long id){
        Player player;
        if (id < 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (id == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try {
            player = repository.findById(id).get();
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return player;
    }

    @Transactional
    public void delete(Player player){
        try {
            repository.delete(player);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public List<Player> getSortedPlayers(List<Player> filteredPlayers, String page, String countOnPage, String order){
        int pageNum = Integer.parseInt(page)+1;
        int count = Integer.parseInt(countOnPage);
        List<Player> sortedPlayers = new ArrayList<>();
        if (order.equalsIgnoreCase("NAME"))
            filteredPlayers.sort(Comparator.comparing(Player::getName));
        else if (order.equalsIgnoreCase("EXPERIENCE"))
            filteredPlayers.sort(Comparator.comparing(Player::getExperience));
        else if (order.equalsIgnoreCase("BIRTHDAY"))
            filteredPlayers.sort(Comparator.comparing(Player::getBirthday));
        for (int i = pageNum*count-(count-1)-1; i < count*pageNum && i < filteredPlayers.size(); i++) {
            sortedPlayers.add(filteredPlayers.get(i));
        }
        return sortedPlayers;
    }


}
