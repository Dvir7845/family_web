"use strict";

const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const { pathToFileURL } = require("node:url");

const REPO_ROOT = path.resolve(__dirname, "..", "..", "..");
const vmModuleCache = new Map();

function transformEsmToCjs(source) {
  const exported = [];

  let code = source.replace(
    /^\s*import\s*\{([^}]+)\}\s*from\s*["']([^"']+)["']\s*;?/gm,
    (_, specifiers, specifierPath) => {
      const bindings = specifiers
        .split(",")
        .map(part => part.trim())
        .filter(Boolean)
        .map(part => part.replace(/\s+as\s+/g, ": "))
        .join(", ");
      return `const { ${bindings} } = __require(${JSON.stringify(specifierPath)});`;
    }
  );

  code = code.replace(
    /^\s*export\s+(async\s+function|function|const|let|var|class)\s+([A-Za-z_$][\w$]*)/gm,
    (_, declaration, name) => {
      exported.push([name, name]);
      return `${declaration} ${name}`;
    }
  );

  code = code.replace(/^\s*export\s*\{([^}]+)\}\s*;?/gm, (_, list) => {
    for (const item of list.split(",")) {
      const trimmed = item.trim();
      if (!trimmed) continue;
      const match = /^([A-Za-z_$][\w$]*)(?:\s+as\s+([A-Za-z_$][\w$]*))?$/.exec(
        trimmed
      );
      if (match) {
        exported.push([match[1], match[2] || match[1]]);
      }
    }
    return "";
  });

  const exportAssignments = exported
    .map(
      ([localName, exportName]) =>
        `__exports[${JSON.stringify(exportName)}] = ${localName};`
    )
    .join("\n");

  return `${code}\n${exportAssignments}\n`;
}

function loadEsmViaVm(absPath) {
  if (vmModuleCache.has(absPath)) {
    return vmModuleCache.get(absPath);
  }

  const exportsObject = {};
  vmModuleCache.set(absPath, exportsObject);

  const source = fs.readFileSync(absPath, "utf8");
  const transformed = transformEsmToCjs(source);
  const wrapped = `(function(__require, __exports) {\n"use strict";\n${transformed}\n})`;
  const fn = vm.runInThisContext(wrapped, { filename: absPath });

  const localRequire = specifier => {
    if (!specifier.startsWith(".")) {
      return require(specifier);
    }
    const targetPath = path.resolve(path.dirname(absPath), specifier);
    return loadEsmViaVm(targetPath);
  };

  fn(localRequire, exportsObject);
  return exportsObject;
}

async function loadEsm(relativePath) {
  const absPath = path.resolve(REPO_ROOT, relativePath);
  try {
    return await import(pathToFileURL(absPath).href);
  } catch (error) {
    if (error instanceof SyntaxError) {
      return loadEsmViaVm(absPath);
    }
    throw error;
  }
}

module.exports = { loadEsm };
