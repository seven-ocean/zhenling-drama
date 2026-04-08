package com.drama.controller;

import com.drama.common.R;
import com.drama.entity.Character;
import com.drama.service.CharacterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    public R<Character> create(@RequestBody Character character) {
        return R.ok(characterService.create(character));
    }

    @GetMapping("/drama/{dramaId}")
    public R<List<Character>> listByDramaId(@PathVariable String dramaId) {
        return R.ok(characterService.listByDramaId(dramaId));
    }

    @GetMapping("/{id}")
    public R<Character> getById(@PathVariable String id) {
        return R.ok(characterService.getById(id));
    }

    @PutMapping("/{id}")
    public R<Character> update(@PathVariable String id, @RequestBody Character character) {
        character.setId(id);
        return R.ok(characterService.update(character));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        characterService.delete(id);
        return R.ok();
    }
}